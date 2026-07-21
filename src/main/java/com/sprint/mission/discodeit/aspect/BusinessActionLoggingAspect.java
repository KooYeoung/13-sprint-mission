package com.sprint.mission.discodeit.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BusinessActionLoggingAspect {

    @Around("@annotation(logAction)")
    public Object logServiceAction(ProceedingJoinPoint joinPoint, LogAction logAction) throws Throwable {
        log.info("{} 시작", logAction.value());
        try {
            Object result = joinPoint.proceed();
            logResult(logAction, joinPoint,result);
            return result;
        } catch (Exception e) {
            log.error("{} 실패", logAction.value(), e);
            throw e;
        }
    }

    private void logResult(LogAction logAction, ProceedingJoinPoint joinPoint,Object result) {
        if (result instanceof LoggableResult loggableResult) {
            log.info("{} 완료. {}", logAction.value(), loggableResult.logFields());
            return;
        }

        if (!logAction.idName().isBlank() && logAction.idParamIndex() >= 0) {
            Object[] args = joinPoint.getArgs();

            if (args.length > logAction.idParamIndex()) {
                log.info("{} 완료. {}={}",
                        logAction.value(),
                        logAction.idName(),
                        args[logAction.idParamIndex()]
                );
                return;
            }
        }

        log.info("{} 완료", logAction.value());
    }
}
