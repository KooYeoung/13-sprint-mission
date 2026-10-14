package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.config.AsyncProperties.PoolProperties;
import com.sprint.mission.discodeit.exception.AsyncExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(AsyncProperties.class)
@RequiredArgsConstructor
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    @ConditionalOnProperty(name = "discodeit.async.enabled", havingValue = "true", matchIfMissing = true)
    static class AsyncActivation {
    }

    private final AsyncProperties properties;
    private final RequestContextTaskDecorator taskDecorator;
    private final AsyncExceptionHandler asyncExceptionHandler;

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler;
    }

    @Bean
    public ThreadPoolTaskExecutor notificationExecutor() {
        return createExecutor("알림", "noti-", properties.notification());
    }

    @Bean
    public ThreadPoolTaskExecutor fileUploadExecutor() {
        return createExecutor("파일 업로드", "file-upload", properties.fileUpload());
    }

    private ThreadPoolTaskExecutor createExecutor(String name, String threadNamePrefix, PoolProperties settings) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(settings.corePoolSize());
        executor.setMaxPoolSize(settings.maxPoolSize());
        executor.setQueueCapacity(settings.queueCapacity());
        executor.setKeepAliveSeconds(Math.toIntExact(settings.keepAlive().getSeconds()));
        executor.setAllowCoreThreadTimeOut(false);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(taskDecorator);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        log.info("{} 실행기 설정을 완료했습니다. corePoolSize={}, maxPoolSize={}, queueCapacity={}, keepAliveSeconds={}",
                name, settings.corePoolSize(), settings.maxPoolSize(), settings.queueCapacity(), settings.keepAlive().getSeconds());
        return executor;
    }
}
