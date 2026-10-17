package dev.canhdominich.boot.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RetryAspects {

    static Object retry(ProceedingJoinPoint point, String label) throws Throwable {
        Throwable last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            Events.record(label + ": attempt " + attempt + ", transaction active = " + TransactionSynchronizationManager.isActualTransactionActive());
            try {
                return point.proceed();
            } catch (IllegalStateException e) {
                last = e;
                Events.record(label + ": attempt " + attempt + " failed");
            }
        }
        throw last;
    }

    @Aspect
    @Component
    @Order(100)
    public static class OutsideTransaction {

        @Around("@annotation(RetryOutside)")
        public Object around(ProceedingJoinPoint point) throws Throwable {
            return retry(point, "retry outside the transaction");
        }
    }

    @Aspect
    @Component
    @Order(300)
    public static class InsideTransaction {

        @Around("@annotation(RetryInside)")
        public Object around(ProceedingJoinPoint point) throws Throwable {
            return retry(point, "retry inside the transaction");
        }
    }
}
