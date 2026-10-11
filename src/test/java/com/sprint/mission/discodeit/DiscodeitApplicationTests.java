package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.config.AdminUserProperties;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.initializer.AdminInitializationLock;
import com.sprint.mission.discodeit.initializer.LocalAdminInitializationLock;
import com.sprint.mission.discodeit.initializer.RedisAdminInitializationLock;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.InMemoryJwtRegistry;
import com.sprint.mission.discodeit.security.JwtRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DiscodeitApplicationTests {

	@Autowired ApplicationContext context;
	@Autowired CacheManager cacheManager;
	@Autowired JwtRegistry jwtRegistry;
	@Autowired AdminUserProperties adminProperties;
	@Autowired UserRepository users;

	@Test
	@DisplayName("Caffeine과 인메모리 JWT 환경은 실제 로컬 락으로 관리자를 생성하고 기동한다")
	void contextLoadsWithLocalLockAndCreatesAdmin() {
		assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
		assertThat(jwtRegistry).isInstanceOf(InMemoryJwtRegistry.class);
		assertThat(context.getBeansOfType(AdminInitializationLock.class).values())
				.hasSize(1).allSatisfy(lock -> assertThat(lock).isInstanceOf(LocalAdminInitializationLock.class));
		assertThat(context.getBeansOfType(RedisAdminInitializationLock.class)).isEmpty();
		assertThat(users.findByUsername(adminProperties.username())).isPresent()
				.get().extracting(user -> user.getRole()).isEqualTo(Role.ADMIN);
	}

}
