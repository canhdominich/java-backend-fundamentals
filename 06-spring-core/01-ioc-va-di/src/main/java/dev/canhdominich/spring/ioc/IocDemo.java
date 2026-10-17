package dev.canhdominich.spring.ioc;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class IocDemo {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        withoutContainer();
        withContainer();
        withFake();
    }

    static String withoutContainer() {
        System.out.println("-- without a container: OrderService creates its own payment");
        Events.clear();
        String result = new HardWiredOrderService().place(100);
        System.out.println("result: " + result);
        return result;
    }

    static List<String> withContainer() {
        System.out.println("-- with a container: the container creates and wires the beans");
        Events.clear();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            String[] userBeans = Arrays.stream(context.getBeanDefinitionNames())
                    .filter(name -> !name.startsWith("org.springframework"))
                    .sorted()
                    .toArray(String[]::new);
            System.out.println("bean names: " + Arrays.toString(userBeans));
            System.out.println("orderService depends on: " + Arrays.toString(context.getBeanFactory().getDependenciesForBean("orderService")));
            System.out.println("paypalPayment is needed by: " + Arrays.toString(context.getBeanFactory().getDependentBeans("paypalPayment")));
            OrderService first = context.getBean(OrderService.class);
            OrderService second = context.getBean(OrderService.class);
            System.out.println("same instance twice: " + (first == second));
            System.out.println("result: " + first.place(100));
        }
        return Events.all();
    }

    static List<Integer> withFake() {
        System.out.println("-- no container at all: a fake payment goes through the constructor");
        FakePayment fake = new FakePayment();
        String result = new OrderService(fake).place(100);
        System.out.println("result: " + result + ", charges recorded by the fake: " + fake.charges());
        return fake.charges();
    }
}
