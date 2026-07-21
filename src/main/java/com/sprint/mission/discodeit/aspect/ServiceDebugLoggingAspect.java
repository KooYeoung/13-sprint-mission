package com.sprint.mission.discodeit.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class ServiceDebugLoggingAspect {

    @Pointcut("execution(* com.sprint.mission.discodeit.service..*.*(..))")
    public void serviceLayer() {

    }

    @Around("serviceLayer()")
    public Object debugLog(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();

        log.debug("{} 시작", method);

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = System.currentTimeMillis() - start;

            log.debug("{} 완료 elapsedMs={}", method, elapsedMs);
            return result;
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - start;
            log.debug("{} 실패 elapsedMs={}, exception={}",
                    method,
                    elapsedMs,
                    e.getClass().getSimpleName()
            );
            throw e;
        }
    }



}
