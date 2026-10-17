package dev.canhdominich.boot.query;

import dev.canhdominich.boot.broken.BrokenApplication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;

public class QueryDemo {

    public static void main(String[] args) {
        run(env("JDBC_URL", "jdbc:mysql://localhost:3307/shop"), env("DB_USER", "app"), env("DB_PASSWORD", "app_pw"));
    }

    static Map<String, Object> properties(String url, String user, String password) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", user);
        properties.put("spring.datasource.password", password);
        properties.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.put("spring.jpa.properties.hibernate.session_factory.statement_inspector", SqlRecorder.class.getName());
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "OFF");
        return properties;
    }

    public static List<String> run(String url, String user, String password) {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(QueryApplication.class);
        application.setDefaultProperties(properties(url, user, password));
        try (ConfigurableApplicationContext context = application.run()) {
            CourseRepository repository = context.getBean(CourseRepository.class);
            TransactionTemplate transaction = context.getBean(TransactionTemplate.class);
            List<Course> seed = new ArrayList<>();
            for (int i = 1; i <= 30; i++) {
                seed.add(new Course("Course " + i, i % 2 == 0 ? "java" : "spring", i % 3 == 0 ? Level.ADVANCED : Level.BASIC, 100_000 + i * 10_000));
            }
            repository.saveAll(seed);
            lines.add("-- query methods derived from the name");
            trace(lines, "findByTitleContainingIgnoreCase(\"course 1\")", () -> repository.findByTitleContainingIgnoreCase("course 1").size() + " rows");
            trace(lines, "findByPriceVndBetween(150000, 200000)", () -> repository.findByPriceVndBetween(150_000, 200_000).size() + " rows");
            trace(lines, "findByLevelInOrderByPriceVndDesc(BASIC, ADVANCED)", () -> repository.findByLevelInOrderByPriceVndDesc(List.of(Level.BASIC, Level.ADVANCED)).get(0).getTitle() + " first");
            trace(lines, "findTop3ByOrderByPriceVndAsc()", () -> repository.findTop3ByOrderByPriceVndAsc().size() + " rows");
            trace(lines, "countByLevel(ADVANCED)", () -> repository.countByLevel(Level.ADVANCED) + " rows");
            trace(lines, "existsByTitle(\"Course 5\")", () -> String.valueOf(repository.existsByTitle("Course 5")));
            lines.add("-- @Query");
            trace(lines, "JPQL expensiveIn(\"java\", 350000)", () -> repository.expensiveIn("java", 350_000).size() + " rows");
            trace(lines, "native cheapestTwoNative(\"java\")", () -> repository.cheapestTwoNative("java").size() + " rows");
            lines.add("-- pagination: what each return type costs");
            trace(lines, "Page: findByLevel(BASIC, page 1, size 5)", () -> {
                Page<Course> page = repository.findByLevel(Level.BASIC, PageRequest.of(1, 5));
                return "content " + page.getContent().size() + ", total " + page.getTotalElements() + ", pages " + page.getTotalPages() + ", number " + page.getNumber();
            });
            trace(lines, "Slice: findSliceByLevel(BASIC, page 1, size 5)", () -> {
                Slice<Course> slice = repository.findSliceByLevel(Level.BASIC, PageRequest.of(1, 5));
                return "content " + slice.getContent().size() + ", hasNext " + slice.hasNext();
            });
            trace(lines, "List: findListByLevel(BASIC, page 1, size 5)", () -> repository.findListByLevel(Level.BASIC, PageRequest.of(1, 5)).size() + " rows");
            trace(lines, "Page with size 100 over 20 matching rows", () -> {
                Page<Course> page = repository.findByLevel(Level.BASIC, PageRequest.of(0, 100));
                return "content " + page.getContent().size() + ", total " + page.getTotalElements();
            });
            lines.add("-- sorting");
            trace(lines, "page 0, size 3, sort by priceVnd descending", () -> repository.findByLevel(Level.BASIC, PageRequest.of(0, 3, Sort.by("priceVnd").descending())).getContent().get(0).getTitle() + " first");
            try {
                repository.findByLevel(Level.BASIC, PageRequest.of(0, 3, Sort.by("nope")));
            } catch (RuntimeException e) {
                lines.add("sort by an unknown property -> " + e.getClass().getSimpleName() + ": " + rootMessage(e));
            }
            lines.add("-- projections");
            trace(lines, "entity: findListByLevel(BASIC, first 1)", () -> repository.findListByLevel(Level.BASIC, PageRequest.of(0, 1)).size() + " row");
            trace(lines, "interface projection findTitlesByLevel", () -> repository.findTitlesByLevel(Level.BASIC).get(0).getTitle());
            trace(lines, "record projection findSummariesByLevel", () -> repository.findSummariesByLevel(Level.BASIC).get(0).toString());
            trace(lines, "dynamic projection findByCategory(\"java\", CourseTitle.class)", () -> repository.findByCategory("java", CourseTitle.class).size() + " rows");
            lines.add("-- bulk update through @Query");
            lines.add("stale entity after a bulk update inside one transaction: " + staleness(repository, transaction, false));
            lines.add("stale entity with clearAutomatically = true: " + staleness(repository, transaction, true));
        }
        lines.add("-- a misspelled property is caught at startup");
        lines.add(brokenStartup(url, user, password));
        lines.forEach(System.out::println);
        return lines;
    }

    private static String staleness(CourseRepository repository, TransactionTemplate transaction, boolean clear) {
        Long id = repository.findByTitleContainingIgnoreCase("Course 3").stream().filter(c -> c.getTitle().equals("Course 3")).findFirst().orElseThrow().getId();
        return transaction.execute(status -> {
            Course course = repository.findById(id).orElseThrow();
            int before = course.getPriceVnd();
            int updated = clear ? repository.raisePricesAndClear(Level.ADVANCED, 1) : repository.raisePrices(Level.ADVANCED, 1);
            int after = repository.findById(id).orElseThrow().getPriceVnd();
            return "price before " + before + ", rows updated " + updated + ", price read again " + after;
        });
    }

    private static String brokenStartup(String url, String user, String password) {
        SpringApplication application = new SpringApplication(BrokenApplication.class);
        application.setDefaultProperties(properties(url, user, password));
        try (ConfigurableApplicationContext context = application.run()) {
            return "started normally";
        } catch (RuntimeException e) {
            return "startup failed: " + e.getClass().getSimpleName() + " / " + rootMessage(e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + (root.getMessage() == null ? "" : root.getMessage().split("\n")[0]);
    }

    private static void trace(List<String> lines, String label, Supplier<String> action) {
        SqlRecorder.clear();
        String result = action.get();
        List<String> statements = SqlRecorder.statements();
        lines.add(label + " -> " + result + "; " + statements.size() + " statements");
        statements.forEach(sql -> lines.add("    " + sql));
        SqlRecorder.clear();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
