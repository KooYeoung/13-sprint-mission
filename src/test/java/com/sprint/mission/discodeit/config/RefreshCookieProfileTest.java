package com.sprint.mission.discodeit.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Refresh 쿠키 설정의 프로파일별 바인딩 테스트")
class RefreshCookieProfileTest {

    @ParameterizedTest
    @CsvSource({"prod,true", "dev,false", "test,false"})
    @DisplayName("실제 YAML에서 프로파일별 Secure 기본값과 공통 쿠키 속성을 바인딩한다")
    void profile_bindsCookieDefaults(String profile, boolean secure) {
        runner(profile, Map.of()).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RefreshCookieProperties.class))
                    .isEqualTo(new RefreshCookieProperties(true, secure, "lax"));
        });
    }

    @ParameterizedTest
    @CsvSource({"prod,false", "dev,true"})
    @DisplayName("명시한 Secure 환경변수가 프로파일 기본값보다 우선한다")
    void suppliedEnvironment_overridesSecureDefault(String profile, boolean secure) {
        runner(profile, Map.of("REFRESH_COOKIE_SECURE", Boolean.toString(secure))).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RefreshCookieProperties.class))
                    .isEqualTo(new RefreshCookieProperties(true, secure, "lax"));
        });
    }

    private ApplicationContextRunner runner(String profile, Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=" + profile,
                        "spring.config.location=classpath:/application.yml")
                .withInitializer(context -> {
                    // 개발자 컴퓨터의 환경변수와 JVM 옵션을 제외하고 각 시나리오의 값만 사용한다.
                    var sources = context.getEnvironment().getPropertySources();
                    sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                    sources.addFirst(new SystemEnvironmentPropertySource("refreshCookieTestEnvironment", environment));
                })
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(CookieConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RefreshCookieProperties.class)
    static class CookieConfiguration {
    }
}
