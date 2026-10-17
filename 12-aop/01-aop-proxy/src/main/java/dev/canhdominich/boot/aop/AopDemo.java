package dev.canhdominich.boot.aop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class AopDemo {

    public static void main(String[] args) {
        run();
    }

    public static List<String> run() {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(AopApplication.class);
        application.setDefaultProperties(Map.of("spring.main.web-application-type", "none", "spring.main.banner-mode", "off", "logging.level.root", "OFF"));
        try (ConfigurableApplicationContext context = application.run()) {
            OrderService orders = context.getBean(OrderService.class);
            PriceCalculator prices = context.getBean(PriceCalculator.class);
            lines.add("-- what the beans really are");
            lines.add("OrderService class: " + describe(orders));
            lines.add("PriceCalculator class: " + describe(prices));
            lines.add("-- advice around a public method with @Audited");
            lines.addAll(trace("place", () -> orders.place("book")));
            lines.add("-- a public method without @Audited");
            lines.addAll(trace("cancel", () -> orders.cancel("book")));
            lines.add("-- a method that calls another through this");
            lines.addAll(trace("outer", () -> orders.outer("book")));
            lines.add("-- a final method");
            lines.addAll(trace("receipt", () -> orders.receipt("book")));
            lines.add("-- an interface-based bean that an aspect advises");
            lines.addAll(trace("priceOf", () -> String.valueOf(prices.priceOf("book"))));
        }
        lines.add("-- the same beans with spring.aop.proxy-target-class=false");
        SpringApplication jdk = new SpringApplication(AopApplication.class);
        jdk.setDefaultProperties(Map.of("spring.main.web-application-type", "none", "spring.main.banner-mode", "off", "logging.level.root", "OFF", "spring.aop.proxy-target-class", "false"));
        try (ConfigurableApplicationContext context = jdk.run()) {
            lines.add("PriceCalculator class: " + describe(context.getBean(PriceCalculator.class)));
            lines.add("OrderService class (no interface): " + describe(context.getBean(OrderService.class)));
            lines.addAll(trace("priceOf", () -> String.valueOf(context.getBean(PriceCalculator.class).priceOf("book"))));
            try {
                context.getBean(PricingService.class);
                lines.add("getBean(PricingService.class): found");
            } catch (RuntimeException e) {
                lines.add("getBean(PricingService.class): " + e.getClass().getSimpleName());
            }
        }
        lines.forEach(System.out::println);
        return lines;
    }

    private static String describe(Object bean) {
        String name = bean.getClass().getSimpleName();
        String kind = AopUtils.isCglibProxy(bean) ? "CGLIB proxy" : AopUtils.isJdkDynamicProxy(bean) ? "JDK dynamic proxy" : "not a proxy";
        return name.replaceAll("\\$Proxy\\d+", "\\$Proxy<n>") + " (" + kind + ", target " + AopUtils.getTargetClass(bean).getSimpleName() + ")";
    }

    private static List<String> trace(String label, java.util.function.Supplier<String> call) {
        Events.clear();
        String result = call.get();
        List<String> lines = new ArrayList<>();
        lines.add(label + " returned " + result);
        Events.all().forEach(event -> lines.add("    " + event));
        return lines;
    }
}
