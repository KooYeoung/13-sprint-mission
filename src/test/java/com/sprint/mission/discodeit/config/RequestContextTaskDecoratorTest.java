package com.sprint.mission.discodeit.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class RequestContextTaskDecoratorTest {
    private final RequestContextTaskDecorator decorator = new RequestContextTaskDecorator();

    @AfterEach
    void clear() { MDC.clear(); SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void capturesSubmissionContextAndClearsWorkerContext(boolean fails) throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> setContext("worker", "worker-user")).get(5, TimeUnit.SECONDS);
            setContext("request", "request-user");
            var callerContext = SecurityContextHolder.getContext();
            Runnable task = decorator.decorate(() -> {
                assertThat(MDC.get("requestId")).isEqualTo("request");
                assertThat(SecurityContextHolder.getContext()).isNotSameAs(callerContext);
                assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("request-user");
                if (fails) throw new IllegalStateException("작업 실패");
            });
            setContext("changed", "changed-user");
            var future = executor.submit(task);
            if (fails) assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
            else future.get(5, TimeUnit.SECONDS);
            executor.submit(() -> {
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            }).get(5, TimeUnit.SECONDS);
            assertThat(MDC.get("requestId")).isEqualTo("changed");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("changed-user");
        } finally { executor.shutdownNow(); }
    }

    @Test
    void reusingThreadDoesNotLeakPreviousRequestsContext() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try {
            setContext("first", "first-user");
            executor.submit(decorator.decorate(() -> assertThat(MDC.get("requestId")).isEqualTo("first"))).get(5, TimeUnit.SECONDS);
            clear();
            executor.submit(decorator.decorate(() -> {
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            })).get(5, TimeUnit.SECONDS);
            executor.submit(() -> {
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            }).get(5, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }

    private void setContext(String requestId, String username) {
        MDC.put("requestId", requestId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
    }
}
