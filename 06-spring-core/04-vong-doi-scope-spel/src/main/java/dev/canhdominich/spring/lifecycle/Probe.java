package dev.canhdominich.spring.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class Probe implements BeanNameAware, InitializingBean, DisposableBean {

    public Probe() {
        Events.record("constructor");
    }

    @Autowired
    public void setEngine(Engine engine) {
        Events.record("dependency injected");
    }

    @Override
    public void setBeanName(String name) {
        Events.record("BeanNameAware.setBeanName(" + name + ")");
    }

    @PostConstruct
    public void postConstruct() {
        Events.record("@PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        Events.record("InitializingBean.afterPropertiesSet");
    }

    public void customInit() {
        Events.record("initMethod");
    }

    @PreDestroy
    public void preDestroy() {
        Events.record("@PreDestroy");
    }

    @Override
    public void destroy() {
        Events.record("DisposableBean.destroy");
    }

    public void customDestroy() {
        Events.record("destroyMethod");
    }
}
