package dev.canhdominich.spring.lifecycle;

import org.springframework.beans.factory.config.BeanPostProcessor;

public class Tracer implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof Probe) {
            Events.record("BeanPostProcessor before initialization");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof Probe) {
            Events.record("BeanPostProcessor after initialization");
        }
        return bean;
    }
}
