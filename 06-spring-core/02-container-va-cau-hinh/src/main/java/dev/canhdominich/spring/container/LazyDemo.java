package dev.canhdominich.spring.container;

import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class LazyDemo {

    public static void main(String[] args) {
        beanFactoryWithoutAutowiring();
        beanFactory();
        applicationContext();
    }

    static String beanFactoryWithoutAutowiring() {
        System.out.println("-- BeanFactory with plain definitions and no autowiring mode");
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("engine", new RootBeanDefinition(Engine.class));
        factory.registerBeanDefinition("car", new RootBeanDefinition(Car.class));
        try {
            factory.getBean("car");
            return "no error";
        } catch (BeansException e) {
            String message = e.getMessage();
            System.out.println("getBean(car) failed: " + message);
            return message;
        }
    }

    static List<String> beanFactory() {
        System.out.println("-- BeanFactory: register two definitions");
        Events.clear();
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("engine", new RootBeanDefinition(Engine.class));
        RootBeanDefinition car = new RootBeanDefinition(Car.class);
        car.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
        factory.registerBeanDefinition("car", car);
        System.out.println("definitions registered, beans created so far: " + Events.all().size());
        factory.getBean("car");
        System.out.println("after getBean(car): " + Events.all());
        return Events.all();
    }

    static List<String> applicationContext() {
        System.out.println("-- ApplicationContext: build the context, call nothing else");
        Events.clear();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EagerConfig.class)) {
            System.out.println("right after the constructor returned: " + Events.all());
            System.out.println("getBean(car) later changes nothing: " + (context.getBean(Car.class) == context.getBean(Car.class)));
        }
        return Events.all();
    }
}
