package com.sprint.mission.discodeit.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BusinessActionLoggingAspect {

    @Around("@annotation(com.sprint.mission.discodeit.aspect.LogAction)")
    public Object logServiceAction(ProceedingJoinPoint joinPoint) throws Throwable {
        LogAction logAction = resolveLogAction(joinPoint);

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

    private LogAction resolveLogAction(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAction logAction = method.getAnnotation(LogAction.class);
        if (logAction != null) {
            return logAction;
        }

        try {
            Method targetMethod = joinPoint.getTarget()
                    .getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
            LogAction targetLogAction = targetMethod.getAnnotation(LogAction.class);
            if (targetLogAction != null) {
                return targetLogAction;
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("LogAction annotation could not be resolved.", e);
        }

        throw new IllegalStateException("LogAction annotation could not be resolved. method=" + signature.toShortString());
    }
}
