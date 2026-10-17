package dev.canhdominich.spring.container;

import dev.canhdominich.spring.container.scanned.ScannedGreeterConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConfigStylesDemo {

    public static void main(String[] args) {
        run();
    }

    public static List<String> run() {
        List<String> lines = new ArrayList<>();
        try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ScannedGreeterConfig.class)) {
            lines.add(describe("component scan", context));
        }
        try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(JavaGreeterConfig.class)) {
            lines.add(describe("@Bean method", context));
        }
        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("greeter.xml")) {
            lines.add(describe("XML", context));
        }
        return lines;
    }

    private static String describe(String style, ConfigurableApplicationContext context) {
        BeanDefinition definition = context.getBeanFactory().getBeanDefinition("greeter");
        String line = style + ": class=" + context.getBean("greeter").getClass().getSimpleName()
                + ", scope=" + (definition.isSingleton() ? "singleton" : definition.getScope())
                + ", greet=\"" + context.getBean(Greeter.class).greet() + "\"";
        System.out.println(line);
        return line;
    }
}
