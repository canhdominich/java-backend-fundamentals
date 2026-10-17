package dev.canhdominich.boot.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(2)
public class LoggingAspect {

    @Around("execution(public * dev.canhdominich.boot.aop.OrderService.*(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Events.record("around: before " + point.getSignature().getName());
        try {
            return point.proceed();
        } finally {
            Events.record("around: after " + point.getSignature().getName());
        }
    }

    @Before("@annotation(audited)")
    public void audit(JoinPoint point, Audited audited) {
        Events.record("before (audited): " + point.getSignature().getName());
    }

    @AfterReturning(pointcut = "@annotation(Audited)", returning = "result")
    public void returned(JoinPoint point, Object result) {
        Events.record("after returning: " + point.getSignature().getName() + " -> " + result);
    }

    @After("@annotation(Audited)")
    public void finished(JoinPoint point) {
        Events.record("after (finally): " + point.getSignature().getName());
    }

    @Before("execution(* dev.canhdominich.boot.aop.PricingService.priceOf(..))")
    public void priced(JoinPoint point) {
        Events.record("before: priceOf");
    }
}
