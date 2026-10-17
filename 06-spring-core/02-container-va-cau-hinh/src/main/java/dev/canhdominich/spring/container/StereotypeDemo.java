package dev.canhdominich.spring.container;

import dev.canhdominich.spring.container.stereo.StereotypeConfig;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

public class StereotypeDemo {

    public static void main(String[] args) {
        run();
    }

    public static List<String> run() {
        for (Class<?> stereotype : List.of(Service.class, Repository.class, Controller.class)) {
            System.out.println("@" + stereotype.getSimpleName() + " is meta-annotated with @Component: " + AnnotatedElementUtils.hasAnnotation(stereotype, Component.class));
        }
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(StereotypeConfig.class)) {
            List<String> names = Arrays.stream(context.getBeanDefinitionNames())
                    .filter(name -> !name.startsWith("org.springframework") && !name.equals("stereotypeConfig"))
                    .sorted()
                    .toList();
            System.out.println("beans found by scanning: " + names);
            System.out.println("beans carrying @Service: " + Arrays.toString(context.getBeanNamesForAnnotation(Service.class)));
            System.out.println("beans carrying @Repository: " + Arrays.toString(context.getBeanNamesForAnnotation(Repository.class)));
            return names;
        }
    }
}
