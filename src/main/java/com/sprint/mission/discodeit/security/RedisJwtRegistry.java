package com.sprint.mission.discodeit.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.exception.jwt.TokenRenewalFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "discodeit.security.jwt",
        name = "registry-type",
        havingValue = "redis"
)
public class RedisJwtRegistry implements JwtRegistry{

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private static final RedisScript<Long> REGISTER_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/register-jwt.lua"),
            Long.class
    );

    private static final RedisScript<Long> ROTATE_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/rotate-jwt.lua"),
            Long.class
    );

    private static final RedisScript<Long> INVALIDATE_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/invalidate-jwt.lua"),
            Long.class
    );

    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.userDto().id();

        long result = executeJwtScript(
                REGISTER_SCRIPT,
                userId,
                serializeJwtInformation(jwtInformation),
                jwtTokenProvider.getMaxActiveJwtCount().toString()
        );

        if(result == -1L){
            throw new IllegalArgumentException("저장 전에 Refresh Token이 만료되었습니다.");
        }

        if(result != 1L){
            throw new IllegalStateException("JWT 등록 결과가 올바르지 않습니다. userId="+userId);
        }
    }

    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        redisTemplate.delete(jwtKey(userId));
    }

    @Override
    public void invalidateJwtInformationByRefreshToken(UUID userId, String refreshToken) {
        long result = executeJwtScript(
                INVALIDATE_SCRIPT,
                userId,
                refreshToken
        );

        if (result != 0L && result != 1L){
            throw new IllegalStateException("JWT 로그아웃 결과가 올바르지 않습니다. userId="+ userId);
        }
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(jwtKey(userId)));
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return hasActiveToken(accessToken, JwtTokenProvider.TokenType.ACCESS);
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return hasActiveToken(refreshToken, JwtTokenProvider.TokenType.REFRESH);
    }

    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        UUID userId = newJwtInformation.userDto().id();

        long result = executeJwtScript(
                ROTATE_SCRIPT,
                userId,
                refreshToken,
                serializeJwtInformation(newJwtInformation),
                jwtTokenProvider.getMaxActiveJwtCount().toString()
        );

        if (result == 0L || result == -1L){
            throw new TokenRenewalFailedException();
        }

        if(result != 1L){
            throw new IllegalStateException("JWT 갱신 결과가 올바르지 않습니다. userId="+userId);
        }
    }

    @Override
    public Set<UUID> findActiveUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()){
            return Set.of();
        }

        List<UUID> distinctUserIds = new ArrayList<>(new LinkedHashSet<>(userIds));
        Set<UUID> activeUserIds = new HashSet<>();

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) con -> {
            distinctUserIds.forEach(userId -> {
                byte[] key = redisTemplate.getStringSerializer().serialize(jwtKey(userId));

                con.keyCommands().exists(new byte[][]{key});
            });
            return null;
        });

        for (int i = 0; i < distinctUserIds.size(); i++) {
            if (Long.valueOf(1L).equals(results.get(i))) {
                activeUserIds.add(distinctUserIds.get(i));
            }
        }

        return activeUserIds;
    }

    @Override
    public Set<UUID> findUserIdsWithValidAccessToken(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Set.of();

        List<UUID> distinctUserIds = new ArrayList<>(new LinkedHashSet<>(userIds));
        // GET을 파이프라인으로 묶어 사용자마다 Redis 왕복 통신이 발생하지 않도록 한다.
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            distinctUserIds.forEach(userId -> {
                byte[] key = redisTemplate.getStringSerializer().serialize(jwtKey(userId));
                connection.stringCommands().get(key);
            });
            return null;
        });

        Set<UUID> onlineUserIds = new HashSet<>();
        long now = clock.millis();
        for (int i = 0; i < distinctUserIds.size(); i++) {
            UUID userId = distinctUserIds.get(i);
            // 없는 키의 null 결과도 위치를 유지하므로 요청 순서대로 연결한다.
            boolean hasValidAccessToken = deserializeStoredTokens(userId, (String) results.get(i)).stream()
                    .filter(info -> info.refreshExpiresAt() > now)
                    .flatMap(info -> jwtTokenProvider.validateToken(info.accessToken()).stream())
                    .filter(claims -> JwtTokenProvider.TokenType.ACCESS.name()
                            .equals(jwtTokenProvider.getTokenType(claims)))
                    .filter(claims -> userId.equals(jwtTokenProvider.getUserId(claims)))
                    .anyMatch(claims -> claims.getExpiration() != null
                            && claims.getExpiration().getTime() > now);
            if (hasValidAccessToken) onlineUserIds.add(userId);
        }
        return onlineUserIds;
    }

    private String jwtKey(UUID userId) {
        return "discodeit:jwt:"+userId;
    }

    private boolean hasActiveToken(String token, JwtTokenProvider.TokenType tokenType){
        return jwtTokenProvider.validateToken(token)
                .filter(claims -> tokenType.name().equals(jwtTokenProvider.getTokenType(claims)))
                .map(claims -> {
                    UUID userId = jwtTokenProvider.getUserId(claims);
                    long now = clock.millis();

                    return readStoredTokens(userId).stream()
                            .filter(info -> info.refreshExpiresAt() > now)
                            .anyMatch(info -> switch (tokenType){
                                case ACCESS -> token.equals(info.accessToken());
                                case REFRESH -> token.equals(info.refreshToken());
                            });
                })
                .orElse(false);
    }

    private List<StoredJwtInformation> readStoredTokens(UUID userId){
        String json = redisTemplate.opsForValue().get(jwtKey(userId));
        return deserializeStoredTokens(userId, json);
    }

    private List<StoredJwtInformation> deserializeStoredTokens(UUID userId, String json) {
        if (json == null) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<StoredJwtInformation>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "JWT 등록 정보를 읽을 수 없습니다. userId=" + userId,
                    e
            );
        }
    }

    private String serializeJwtInformation(JwtInformation information){
        return jwtTokenProvider.validateToken(information.refreshToken())
                .filter(claims -> JwtTokenProvider.TokenType.REFRESH.name()
                        .equals(jwtTokenProvider.getTokenType(claims)))
                .filter(claims -> information.userDto().id()
                        .equals(jwtTokenProvider.getUserId(claims)))
                .filter(claims -> claims.getExpiration() != null)
                .map(claims -> new StoredJwtInformation(
                        information.accessToken(),
                        information.refreshToken(),
                        claims.getExpiration().getTime()
                ))
                .map(stored -> convertStoredJwtToJson(information.userDto().id(), stored))
                .orElseThrow(() -> new IllegalArgumentException("등록할 Refresh Token이 올바르지 않습니다."))
                ;
    }

    private String convertStoredJwtToJson(UUID userId, StoredJwtInformation stored) {
        try {
            return objectMapper.writeValueAsString(stored);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "JWT 등록 정보를 변환할 수 없습니다. userId="
                            + userId,
                    e
            );
        }
    }

    private long executeJwtScript(RedisScript<Long> script, UUID userId, Object... arguments){
        Long result = redisTemplate.execute(script, List.of(jwtKey(userId)), arguments);

        if(result == null){
            throw new IllegalArgumentException("JWT 처리 결과를 확인할 수 없습니다. userId=" + userId);
        }

        return result;
    }

    private record StoredJwtInformation(
            String accessToken,
            String refreshToken,
            long refreshExpiresAt
    ) {
    }
}
