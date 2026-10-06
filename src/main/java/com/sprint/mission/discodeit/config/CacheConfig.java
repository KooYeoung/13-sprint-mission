package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String CHANNELS = "channels";
    public static final String USERS_BY_NOTIFICATIONS = "usersByNotifications";
    public static final String USERS = "users";

    @Bean
    public KeyGenerator currentUserIdKeyGenerator() {
        return (target, method, params) -> {
            DiscodeitUserDetails principal = (DiscodeitUserDetails) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            return principal.getUserDto().id();
        };
    }

}
