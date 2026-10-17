package dev.canhdominich.boot.aop;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(50)
public class AdviceAspect {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Around("@annotation(Traced)")
    public Object trace(ProceedingJoinPoint point) throws Throwable {
        Events.record("around: before");
        try {
            return point.proceed();
        } finally {
            Events.record("around: after");
        }
    }

    @Before("@annotation(Traced)")
    public void before(JoinPoint point) {
        Events.record("before");
    }

    @AfterReturning(pointcut = "@annotation(Traced)", returning = "result")
    public void afterReturning(Object result) {
        Events.record("after returning: " + result);
    }

    @AfterThrowing(pointcut = "@annotation(Traced)", throwing = "error")
    public void afterThrowing(Throwable error) {
        Events.record("after throwing: " + error.getMessage());
    }

    @After("@annotation(Traced)")
    public void after() {
        Events.record("after (finally)");
    }

    @Around("@annotation(Capped)")
    public Object cap(ProceedingJoinPoint point) throws Throwable {
        Object result = point.proceed();
        return ((String) result).toUpperCase();
    }

    @Around("@annotation(Cached)")
    public Object cache(ProceedingJoinPoint point) throws Throwable {
        String key = point.getSignature().getName() + java.util.Arrays.toString(point.getArgs());
        Object hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        Object value = point.proceed();
        cache.put(key, value);
        return value;
    }
}
