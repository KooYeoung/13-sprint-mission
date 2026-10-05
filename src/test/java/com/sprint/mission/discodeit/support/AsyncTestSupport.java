package com.sprint.mission.discodeit.support;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Duration;
import static org.awaitility.Awaitility.await;

public final class AsyncTestSupport {
    private AsyncTestSupport() {}

    public static void awaitIdle(ThreadPoolTaskExecutor... executors) {
        await().pollInterval(Duration.ofMillis(20)).atMost(Duration.ofSeconds(15)).until(() -> {
            for (ThreadPoolTaskExecutor executor : executors) {
                if (executor.getActiveCount() != 0 || !executor.getThreadPoolExecutor().getQueue().isEmpty()) return false;
            }
            return true;
        });
    }
}
