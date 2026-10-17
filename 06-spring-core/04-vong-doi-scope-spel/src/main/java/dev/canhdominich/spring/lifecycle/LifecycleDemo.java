package dev.canhdominich.spring.lifecycle;

import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class LifecycleDemo {

    public static void main(String[] args) {
        run();
    }

    public static List<String> run() {
        Events.clear();
        System.out.println("-- creating the context");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LifecycleConfig.class);
        System.out.println("-- context is up, bean is ready to use");
        context.getBean(Probe.class);
        System.out.println("-- closing the context");
        context.close();
        return Events.all();
    }
}
