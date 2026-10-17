package dev.canhdominich.spring.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpelDemo {

    public static void main(String[] args) {
        expressions();
        valueAnnotation();
    }

    public record Roster(List<Student> students) {

        public List<Student> getStudents() {
            return students;
        }
    }

    public static List<String> expressions() {
        ExpressionParser parser = new SpelExpressionParser();
        Student an = new Student("An", "ADVANCED", 8, null, new Student.Address("Ha Noi"));
        Student binh = new Student("Binh", "BASIC", 4, "Bi", null);
        Roster roster = new Roster(List.of(an, binh));
        List<String> lines = new ArrayList<>();
        lines.add(show(parser, null, "2 * 60 * 60"));
        lines.add(show(parser, null, "'Hello'.length()"));
        lines.add(show(parser, null, "T(java.lang.Math).max(3, 7)"));
        lines.add(show(parser, binh, "name"));
        lines.add(show(parser, binh, "score >= 5 ? 'pass' : 'fail'"));
        lines.add(show(parser, an, "nickname ?: 'none'"));
        lines.add(show(parser, binh, "address?.city"));
        lines.add(show(parser, binh, "address.city"));
        lines.add(show(parser, roster, "students.?[level == 'ADVANCED'].![name]"));
        lines.add(show(parser, roster, "students.![score]"));
        return lines;
    }

    private static String show(ExpressionParser parser, Object root, String expression) {
        String result;
        try {
            StandardEvaluationContext context = root == null ? new StandardEvaluationContext() : new StandardEvaluationContext(root);
            result = String.valueOf(parser.parseExpression(expression).getValue(context));
        } catch (SpelEvaluationException e) {
            result = e.getClass().getSimpleName() + " " + e.getMessage();
        }
        String line = expression + "  ->  " + result;
        System.out.println(line);
        return line;
    }

    public static class Greeter {

        public String getPrefix() {
            return "hi";
        }

        public String getNothing() {
            return null;
        }
    }

    public static class Settings {

        @Value("${report.hours}")
        int hours;

        @Value("#{${report.hours} * 3600}")
        int seconds;

        @Value("#{2 * 60 * 60}")
        int cacheSeconds;

        @Value("#{greeter.prefix.toUpperCase()}")
        String shout;

        @Value("#{greeter.nothing?.toUpperCase()}")
        String safe;

        @Value("#{T(java.lang.Math).max(3, 7)}")
        int biggest;
    }

    @Configuration
    @PropertySource("classpath:report.properties")
    static class SettingsConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        Greeter greeter() {
            return new Greeter();
        }

        @Bean
        Settings settings() {
            return new Settings();
        }
    }

    public static String valueAnnotation() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SettingsConfig.class)) {
            Settings s = context.getBean(Settings.class);
            String line = "hours=" + s.hours + ", seconds=" + s.seconds + ", cacheSeconds=" + s.cacheSeconds + ", shout=" + s.shout + ", safe=" + s.safe + ", biggest=" + s.biggest;
            System.out.println(line);
            return line;
        }
    }
}
