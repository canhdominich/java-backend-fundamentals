package dev.canhdominich.boot.config;

import dev.canhdominich.boot.other.OverrideConfig;
import dev.canhdominich.boot.outside.OutsideComponent;
import dev.canhdominich.boot.starter.GreetingService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ConfigDemo {

    public static void main(String[] args) throws Exception {
        run().forEach(System.out::println);
    }

    public static List<String> run() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("-- binding: application.properties into one typed object");
        lines.add(with(context -> context.getBean(AppProperties.class).toString()));
        lines.add("-- relaxed names on the command line");
        for (String arg : List.of("--app.max-size=9", "--app.maxSize=7", "--app.max_size=8")) {
            lines.add(arg + " -> maxSize " + with(context -> String.valueOf(context.getBean(AppProperties.class).maxSize()), arg));
        }
        lines.add("-- precedence: which source wins for app.name");
        lines.add("application.properties only -> " + name());
        lines.add("profile dev -> " + name("--spring.profiles.active=dev"));
        lines.add("profile dev + system property -Dapp.name=sys -> " + withSystem("sys", "--spring.profiles.active=dev"));
        lines.add("profile dev + environment APP_NAME=env -> " + child("env", "--spring.profiles.active=dev"));
        lines.add("profile dev + environment APP_NAME=env + --app.name=cli -> " + child("env", "--spring.profiles.active=dev", "--app.name=cli"));
        lines.add("-- profiles: which beans exist");
        lines.add("no profile: DevOnly present = " + with(context -> String.valueOf(context.getBeansOfType(DevOnly.class).size() == 1)));
        lines.add("profile dev: DevOnly present = " + with(context -> String.valueOf(context.getBeansOfType(DevOnly.class).size() == 1), "--spring.profiles.active=dev"));
        lines.add("profile dev: maxSize = " + with(context -> String.valueOf(context.getBean(AppProperties.class).maxSize()), "--spring.profiles.active=dev"));
        lines.add("-- failing fast on bad configuration");
        lines.add("--app.max-size=0 -> " + failure(ConfigApplication.class, "--app.max-size=0"));
        lines.add("--app.timeout=abc -> " + failure(ConfigApplication.class, "--app.timeout=abc"));
        lines.add("@Value(\"${app.missing}\") -> " + failure(MissingValueHolder.class));
        lines.add("-- a small auto-configuration of our own");
        lines.add("default -> " + with(context -> context.getBean(GreetingService.class).greet("An")));
        lines.add("--greeting.message=hi -> " + with(context -> context.getBean(GreetingService.class).greet("An"), "--greeting.message=hi"));
        lines.add("--greeting.enabled=false -> GreetingService beans: " + with(context -> String.valueOf(context.getBeansOfType(GreetingService.class).size()), "--greeting.enabled=false"));
        lines.add("own GreetingService bean in OverrideConfig -> " + with(context -> context.getBean(GreetingService.class).greet("An"), new Class<?>[] {OverrideConfig.class}));
        lines.add("condition report for GreetingAutoConfiguration: " + with(context -> {
            ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
            return report.getConditionAndOutcomesBySource().entrySet().stream()
                    .filter(entry -> entry.getKey().endsWith("GreetingAutoConfiguration"))
                    .map(entry -> entry.getValue().isFullMatch() ? "matched" : "not matched")
                    .findFirst().orElse("not evaluated");
        }));
        lines.add("-- what component scanning covers");
        lines.add("OutsideComponent beans (package dev.canhdominich.boot.outside) = " + with(context -> String.valueOf(context.getBeansOfType(OutsideComponent.class).size())));
        return lines;
    }

    static class MissingValueHolder extends ConfigApplication {
    }

    private static String name(String... args) {
        return with(context -> context.getBean(AppProperties.class).name(), args);
    }

    private static String withSystem(String value, String... args) {
        System.setProperty("app.name", value);
        try {
            return name(args);
        } finally {
            System.clearProperty("app.name");
        }
    }

    private static String with(Function<ConfigurableApplicationContext, String> inspector, String... args) {
        return with(inspector, new Class<?>[] {}, args);
    }

    private static String with(Function<ConfigurableApplicationContext, String> inspector, Class<?>[] extra, String... args) {
        List<Class<?>> sources = new ArrayList<>(List.of(ConfigApplication.class));
        sources.addAll(List.of(extra));
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(sources.toArray(new Class<?>[0]))
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .properties("spring.main.banner-mode=off", "logging.level.root=OFF")
                .run(args)) {
            return inspector.apply(context);
        }
    }

    private static String failure(Class<?> source, String... args) {
        List<Class<?>> sources = new ArrayList<>(List.of(ConfigApplication.class));
        if (source == MissingValueHolder.class) {
            sources.add(MissingValue.class);
        }
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(sources.toArray(new Class<?>[0]))
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .properties("spring.main.banner-mode=off", "logging.level.root=OFF")
                .run(args)) {
            return "started normally";
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String message = root.getMessage() == null ? "" : root.getMessage().split("\n")[0];
            return e.getClass().getSimpleName() + " / " + root.getClass().getSimpleName() + ": " + message;
        }
    }

    private static String child(String envName, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java", "-cp", System.getProperty("java.class.path"), PrintName.class.getName()));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("APP_NAME", envName);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        return output.lines().filter(line -> line.startsWith("name=")).findFirst().map(line -> line.substring("name=".length()).replaceAll(" maxSize=.*", "")).orElse(output.trim());
    }
}
