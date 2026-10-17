package dev.canhdominich.boot.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

public class RepositoryDemo {

    public static void main(String[] args) {
        run(env("JDBC_URL", "jdbc:mysql://localhost:3307/shop"), env("DB_USER", "app"), env("DB_PASSWORD", "app_pw")).forEach(System.out::println);
    }

    public static List<String> run(String url, String user, String password) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", user);
        properties.put("spring.datasource.password", password);
        properties.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.put("spring.jpa.properties.hibernate.session_factory.statement_inspector", SqlRecorder.class.getName());
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "OFF");
        SpringApplication application = new SpringApplication(JpaApplication.class);
        application.setDefaultProperties(properties);
        List<String> lines = new ArrayList<>();
        try (ConfigurableApplicationContext context = application.run()) {
            StudentRepository repository = context.getBean(StudentRepository.class);
            TransactionTemplate transaction = context.getBean(TransactionTemplate.class);
            lines.add("repository object: " + repository.getClass().getSimpleName().replaceAll("[0-9]+$", "") + " (proxy), target " + AopProxyUtils.ultimateTargetClass(repository).getSimpleName());
            lines.add("-- save");
            SqlRecorder.clear();
            Student an = repository.save(new Student("An", "an@example.com", Level.BASIC));
            report(lines, "save(new Student) gives id " + (an.getId() != null));
            Student found = repository.findById(an.getId()).orElseThrow();
            found.setName("An Nguyen");
            SqlRecorder.clear();
            repository.save(found);
            report(lines, "save(detached student with an id)");
            SqlRecorder.clear();
            transaction.executeWithoutResult(status -> repository.findById(an.getId()).orElseThrow().setName("An N."));
            report(lines, "change inside one transaction, no save call");
            lines.add("-- reading");
            SqlRecorder.clear();
            repository.findById(an.getId());
            report(lines, "findById");
            SqlRecorder.clear();
            repository.existsById(an.getId());
            report(lines, "existsById");
            SqlRecorder.clear();
            repository.count();
            report(lines, "count");
            SqlRecorder.clear();
            transaction.executeWithoutResult(status -> repository.getReferenceById(an.getId()));
            report(lines, "getReferenceById, id only used");
            SqlRecorder.clear();
            transaction.executeWithoutResult(status -> repository.getReferenceById(an.getId()).getName());
            report(lines, "getReferenceById, then getName");
            lines.add("-- derived queries");
            SqlRecorder.clear();
            repository.findByEmail("an@example.com");
            report(lines, "findByEmail");
            SqlRecorder.clear();
            repository.findByLevel(Level.BASIC);
            report(lines, "findByLevel");
            lines.add("-- saving and deleting several");
            SqlRecorder.clear();
            repository.saveAll(List.of(new Student("B1", "b1@example.com", Level.BASIC), new Student("B2", "b2@example.com", Level.BASIC), new Student("B3", "b3@example.com", Level.ADVANCED)));
            report(lines, "saveAll of 3");
            SqlRecorder.clear();
            repository.deleteById(an.getId());
            report(lines, "deleteById");
            SqlRecorder.clear();
            repository.deleteAllInBatch();
            report(lines, "deleteAllInBatch");
            lines.add("-- a duplicate email");
            repository.save(new Student("C", "c@example.com", Level.BASIC));
            try {
                repository.save(new Student("C again", "c@example.com", Level.BASIC));
            } catch (RuntimeException e) {
                List<String> chain = new ArrayList<>();
                for (Throwable t = e; t != null; t = t.getCause()) {
                    chain.add(t.getClass().getSimpleName());
                }
                lines.add("exception chain: " + chain);
            }
        }
        return lines;
    }

    private static void report(List<String> lines, String label) {
        List<String> statements = SqlRecorder.statements();
        lines.add(label + " -> " + statements.size() + " statements");
        statements.forEach(sql -> lines.add("    " + sql));
        SqlRecorder.clear();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
