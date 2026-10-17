package dev.canhdominich.boot.aop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class AdviceDemo {

    public static void main(String[] args) {
        run();
    }

    public static List<String> run() {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(AdviceApplication.class);
        application.setDefaultProperties(Map.of("spring.main.web-application-type", "none", "spring.main.banner-mode", "off", "logging.level.root", "OFF",
                "spring.datasource.url", "jdbc:h2:mem:advice;DB_CLOSE_DELAY=-1"));
        try (ConfigurableApplicationContext context = application.run()) {
            PaymentService payments = context.getBean(PaymentService.class);
            FlakyWriter writer = context.getBean(FlakyWriter.class);
            lines.add("-- the five kinds of advice, success and failure");
            lines.addAll(trace("charge(100)", () -> payments.charge(100)));
            lines.addAll(trace("charge(0)", () -> payments.charge(0)));
            lines.add("-- what @Around can do that the others cannot");
            lines.add("changes the result: " + payments.receiptNumber());
            int first = payments.priceOf("book");
            int second = payments.priceOf("book");
            lines.add("skips the call: priceOf twice gave " + first + " and " + second + ", target ran " + payments.lookups() + " time(s)");
            lines.add("-- @Transactional is an aspect too");
            List<String> advisors = new ArrayList<>();
            for (Advisor advisor : ((Advised) writer).getAdvisors()) {
                advisors.add(advisor.getAdvice().getClass().getSimpleName());
            }
            lines.add("advice objects on the FlakyWriter proxy: " + advisors);
            lines.add("-- retry outside the transaction (aspect order 100, transactions at 200)");
            lines.addAll(trace("saveWithRetryOutsideTransaction", () -> writer.saveWithRetryOutsideTransaction("outside")));
            lines.add("rows saved: " + writer.count("outside"));
            lines.add("-- retry inside the transaction (aspect order 300)");
            lines.addAll(trace("saveWithRetryInsideTransaction", () -> writer.saveWithRetryInsideTransaction("inside")));
            lines.add("rows saved: " + writer.count("inside"));
        }
        lines.forEach(System.out::println);
        return lines;
    }

    private static List<String> trace(String label, Supplier<String> call) {
        Events.clear();
        List<String> lines = new ArrayList<>();
        String outcome;
        try {
            outcome = "returned " + call.get();
        } catch (RuntimeException e) {
            outcome = "threw " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        lines.add(label + " " + outcome);
        Events.all().forEach(event -> lines.add("    " + event));
        return lines;
    }
}
