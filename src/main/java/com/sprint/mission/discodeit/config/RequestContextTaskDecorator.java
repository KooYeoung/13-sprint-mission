package com.sprint.mission.discodeit.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RequestContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {

        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        SecurityContext capturedContext = SecurityContextHolder.createEmptyContext();
        capturedContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());

        return () -> {
            try {
                applyMdc(mdcContext);
                SecurityContextHolder.setContext(capturedContext);
                runnable.run();
            } finally {
                MDC.clear();
                SecurityContextHolder.clearContext();
            }
        };
    }

    private void applyMdc(Map<String, String> context) {
        if (context == null) MDC.clear();
        else MDC.setContextMap(context);
    }
}
