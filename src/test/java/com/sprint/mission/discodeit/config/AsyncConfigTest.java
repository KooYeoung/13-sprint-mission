package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.config.AsyncProperties.PoolProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("비동기 실행기 설정 바인딩과 빈 생명주기")
class AsyncConfigTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void asyncSwitchControlsProxyExecution(boolean enabled) {
        yamlRunner("dev", Map.of()).withPropertyValues("discodeit.async.enabled=" + enabled)
                .withBean(AsyncProbe.class).run(context -> {
                    assertThat(context).hasNotFailed();
                    String caller = Thread.currentThread().getName();
                    String worker = context.getBean(AsyncProbe.class).thread().get(5, TimeUnit.SECONDS);
                    if (enabled) assertThat(worker).startsWith("noti-").isNotEqualTo(caller);
                    else assertThat(worker).isEqualTo(caller);
                });
    }

    public static class AsyncProbe {
        @org.springframework.scheduling.annotation.Async("notificationExecutor")
        public java.util.concurrent.CompletableFuture<String> thread() {
            return java.util.concurrent.CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }

    @Test
    @DisplayName("필수 설정이 누락되면 임의의 값으로 시작하지 않는다")
    void failsWithoutRequiredProperties() {
        runner(Map.of()).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("discodeit.async");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    @DisplayName("실제 YAML의 풀별 기본값으로 두 실행기를 생성한다")
    void loadsApplicationYaml(String profile) {
        yamlRunner(profile, Map.of()).run(context -> {
            assertThat(context).hasNotFailed();
            AsyncProperties properties = context.getBean(AsyncProperties.class);
            assertThat(properties.notification()).isEqualTo(new PoolProperties(2, 4, 32, Duration.ofSeconds(60)));
            assertThat(properties.fileUpload()).isEqualTo(new PoolProperties(2, 4, 4, Duration.ofSeconds(60)));

            ThreadPoolTaskExecutor notification = context.getBean("notificationExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor upload = context.getBean("fileUploadExecutor", ThreadPoolTaskExecutor.class);
            assertThat(notification.getCorePoolSize()).isEqualTo(2);
            assertThat(notification.getMaxPoolSize()).isEqualTo(4);
            assertThat(notification.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(32);
            assertThat(notification.getKeepAliveSeconds()).isEqualTo(60);
            assertThat(upload.getCorePoolSize()).isEqualTo(2);
            assertThat(upload.getMaxPoolSize()).isEqualTo(4);
            assertThat(upload.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(4);
            assertThat(upload.getKeepAliveSeconds()).isEqualTo(60);
        });
    }

    @Test
    @DisplayName("환경변수가 YAML보다 우선하고 두 풀에 서로 다른 설정을 적용한다")
    void bindsEnvironmentOverridesIndependently() {
        Map<String, Object> environment = Map.of(
                "DISCODEIT_ASYNC_NOTIFICATION_COREPOOLSIZE", "3",
                "DISCODEIT_ASYNC_NOTIFICATION_MAXPOOLSIZE", "6",
                "DISCODEIT_ASYNC_NOTIFICATION_QUEUECAPACITY", "9",
                "DISCODEIT_ASYNC_NOTIFICATION_KEEPALIVE", "90s",
                "DISCODEIT_ASYNC_FILEUPLOAD_COREPOOLSIZE", "2",
                "DISCODEIT_ASYNC_FILEUPLOAD_MAXPOOLSIZE", "5",
                "DISCODEIT_ASYNC_FILEUPLOAD_QUEUECAPACITY", "0",
                "DISCODEIT_ASYNC_FILEUPLOAD_KEEPALIVE", "2m"
        );

        yamlRunner("prod", environment).run(context -> {
            assertThat(context).hasNotFailed();
            ThreadPoolTaskExecutor notification = context.getBean("notificationExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor upload = context.getBean("fileUploadExecutor", ThreadPoolTaskExecutor.class);
            assertThat(notification.getCorePoolSize()).isEqualTo(3);
            assertThat(notification.getMaxPoolSize()).isEqualTo(6);
            assertThat(notification.getKeepAliveSeconds()).isEqualTo(90);
            assertThat(notification.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(9);
            assertThat(upload.getCorePoolSize()).isEqualTo(2);
            assertThat(upload.getMaxPoolSize()).isEqualTo(5);
            assertThat(upload.getKeepAliveSeconds()).isEqualTo(120);
            assertThat(upload.getQueueCapacity()).isZero();
            assertThat(upload.getThreadPoolExecutor().getQueue()).isInstanceOf(SynchronousQueue.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DISCODEIT_ASYNC_NOTIFICATION_COREPOOLSIZE", "DISCODEIT_ASYNC_NOTIFICATION_MAXPOOLSIZE",
            "DISCODEIT_ASYNC_FILEUPLOAD_QUEUECAPACITY", "DISCODEIT_ASYNC_FILEUPLOAD_KEEPALIVE"
    })
    @DisplayName("직접 전달한 빈 필수 설정은 시작 실패로 처리한다")
    void rejectsEmptyEnvironmentValue(String variableName) {
        yamlRunner("dev", Map.of(variableName, "")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("discodeit.async");
        });
    }

    @Test
    @DisplayName("한 항목만 재정의하면 나머지는 YAML 기본값을 유지한다")
    void keepsYamlDefaultsForPartialOverride() {
        yamlRunner("dev", Map.of("DISCODEIT_ASYNC_NOTIFICATION_COREPOOLSIZE", "3")).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AsyncProperties.class).notification())
                    .isEqualTo(new PoolProperties(3, 4, 32, Duration.ofSeconds(60)));
            ThreadPoolTaskExecutor notification = context.getBean("notificationExecutor", ThreadPoolTaskExecutor.class);
            assertThat(notification.getCorePoolSize()).isEqualTo(3);
            assertThat(notification.getMaxPoolSize()).isEqualTo(4);
            assertThat(notification.getQueueCapacity()).isEqualTo(32);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "discodeit.async.notification.core-pool-size, 0",
            "discodeit.async.notification.max-pool-size, 2",
            "discodeit.async.notification.queue-capacity, -1",
            "discodeit.async.notification.keep-alive, 0s",
            "discodeit.async.file-upload.keep-alive, -1s",
            "discodeit.async.file-upload.keep-alive, 1500ms",
            "discodeit.async.file-upload.keep-alive, 2147483648s",
            "discodeit.async.file-upload.queue-capacity, -1",
            "discodeit.async.file-upload.max-pool-size, invalid",
            "discodeit.async.file-upload.keep-alive, invalid"
    })
    @DisplayName("잘못된 설정은 적용 대상 속성을 확인할 수 있는 시작 실패로 처리한다")
    void failsStartupForInvalidConfiguration(String property, String value) {
        yamlRunner("dev", Map.of()).withPropertyValues(
                "discodeit.async.notification.core-pool-size=3",
                "discodeit.async.notification.max-pool-size=6",
                property + "=" + value
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("discodeit.async")
                    .hasStackTraceContaining(property.substring(property.lastIndexOf('.') + 1));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"1s", "2147483647s"})
    @DisplayName("실행기가 지원하는 유지 시간의 경계값을 허용한다")
    void acceptsKeepAliveBoundaries(String keepAlive) {
        yamlRunner("dev", Map.of("DISCODEIT_ASYNC_NOTIFICATION_KEEPALIVE", keepAlive)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("notificationExecutor", ThreadPoolTaskExecutor.class).getKeepAliveSeconds())
                    .isEqualTo(Integer.parseInt(keepAlive.substring(0, keepAlive.length() - 1)));
        });
    }

    @Test
    @DisplayName("Spring이 실행기를 초기화하고 컨텍스트 종료 시 두 실행기를 종료한다")
    void managesExecutorLifecycle() {
        AtomicReference<ThreadPoolTaskExecutor> notificationReference = new AtomicReference<>();
        AtomicReference<ThreadPoolTaskExecutor> uploadReference = new AtomicReference<>();

        yamlRunner("dev", Map.of()).run(context -> {
            assertThat(context).hasNotFailed();
            ThreadPoolTaskExecutor notification = context.getBean("notificationExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor upload = context.getBean("fileUploadExecutor", ThreadPoolTaskExecutor.class);
            notificationReference.set(notification);
            uploadReference.set(upload);

            assertThat(notification.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS)).startsWith("noti-");
            assertThat(upload.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS)).startsWith("file-upload");
            assertThat(notification.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
            assertThat(upload.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
            assertThat(notification.getThreadPoolExecutor().allowsCoreThreadTimeOut()).isFalse();
            assertThat(upload.getThreadPoolExecutor().allowsCoreThreadTimeOut()).isFalse();
        });

        assertThat(notificationReference.get().getThreadPoolExecutor().isShutdown()).isTrue();
        assertThat(uploadReference.get().getThreadPoolExecutor().isShutdown()).isTrue();
    }

    private ApplicationContextRunner runner(Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withInitializer(context -> {
                    var sources = context.getEnvironment().getPropertySources();
                    sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                    sources.addFirst(new SystemEnvironmentPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment));
                })
                .withUserConfiguration(AsyncConfig.class, RequestContextTaskDecorator.class,
                        com.sprint.mission.discodeit.exception.AsyncExceptionHandler.class);
    }

    private ApplicationContextRunner yamlRunner(String profile, Map<String, Object> environment) {
        return runner(environment)
                .withPropertyValues("spring.profiles.active=" + profile,
                        "spring.config.location=classpath:/application.yml")
                .withInitializer(new ConfigDataApplicationContextInitializer());
    }
}
