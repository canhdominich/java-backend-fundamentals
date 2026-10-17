package dev.canhdominich.boot.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class OuterAspect {

    @Around("execution(* dev.canhdominich.boot.aop.OrderService.cancel(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Events.record("outer aspect: before " + point.getSignature().getName());
        try {
            return point.proceed();
        } finally {
            Events.record("outer aspect: after " + point.getSignature().getName());
        }
    }
}
