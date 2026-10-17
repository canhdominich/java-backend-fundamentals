package dev.canhdominich.boot.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class MappingDemo {

    public static void main(String[] args) throws Exception {
        run(env("JDBC_URL", "jdbc:mysql://localhost:3307/shop"), env("DB_USER", "app"), env("DB_PASSWORD", "app_pw"));
    }

    static Map<String, Object> properties(String url, String user, String password) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", user);
        properties.put("spring.datasource.password", password);
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "OFF");
        return properties;
    }

    public static List<String> run(String url, String user, String password) throws Exception {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(MappingApplication.class);
        application.setDefaultProperties(properties(url, user, password));
        try (ConfigurableApplicationContext context = application.run()) {
            CatalogDao dao = context.getBean(CatalogDao.class);
            dao.createSchema();
            long spring = dao.add("Spring Boot", "spring", 500_000, 10);
            long data = dao.add("Spring Data JPA", "spring", 450_000, null);
            long java = dao.add("Java Core", "java", 300_000, 5);
            dao.addNullable("JDBC", "java", 250_000);
            dao.addLesson(spring, "Starters");
            dao.addLesson(spring, "Properties");
            dao.addLesson(java, "Types");
            lines.add("-- four ways to turn a row into an object");
            lines.add("lambda RowMapper -> " + dao.withLambda().get(1));
            lines.add("BeanPropertyRowMapper (price_vnd becomes priceVnd) -> priceVnd " + dao.withBeanMapper().get(1).getPriceVnd() + ", title " + dao.withBeanMapper().get(1).getTitle());
            lines.add("DataClassRowMapper into a record -> " + dao.withDataClassMapper().get(1));
            lines.add("queryForList(String.class) -> " + dao.titles());
            lines.add("-- a nullable column");
            lines.add("Integer component, NULL -> " + dao.withDataClassMapper().get(1).discount());
            try {
                dao.withPrimitiveOnNullableColumn();
                lines.add("int component, NULL -> no exception");
            } catch (RuntimeException e) {
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                lines.add("int component, NULL -> " + e.getClass().getSimpleName() + ": " + (root.getMessage() == null ? "" : root.getMessage().split("\n")[0]));
            }
            lines.add("-- grouping a join with a ResultSetExtractor");
            lines.add("lessonsByCourse -> " + dao.lessonsByCourse());
            lines.add("-- named parameters");
            lines.add("category = :category -> " + dao.byCategoryNamed("java"));
            lines.add("in (:categories) with a list of two -> " + dao.inCategories(List.of("java", "spring")).size() + " titles");
            lines.add("the same :v used twice, count -> " + dao.sameNameTwice("java"));
            lines.add("hostile value ' or 1=1 -- -> " + dao.byCategoryNamed("' or 1=1 --").size() + " rows");
            lines.add("-- JdbcClient");
            lines.add("query(String.class).list() -> " + dao.byCategoryClient("spring"));
            lines.add("query(CourseRecord.class).single() -> " + dao.oneByIdClient(data));
            lines.add("optional() for a missing id -> " + dao.optionalByIdClient(9999));
        }
        lines.add("-- batchUpdate of 2000 rows");
        lines.add(batch(url, user, password, "default URL"));
        lines.add(batch(url + (url.contains("?") ? "&" : "?") + "rewriteBatchedStatements=true", user, password, "rewriteBatchedStatements=true"));
        lines.forEach(System.out::println);
        return lines;
    }

    private static String batch(String url, String user, String password, String label) throws Exception {
        SpringApplication application = new SpringApplication(MappingApplication.class);
        application.setDefaultProperties(properties(url, user, password));
        try (ConfigurableApplicationContext context = application.run()) {
            CatalogDao dao = context.getBean(CatalogDao.class);
            dao.createSchema();
            List<String> titles = new ArrayList<>();
            for (int i = 0; i < 2000; i++) {
                titles.add("Batch " + i);
            }
            long before = comInsert(url, user, password);
            long start = System.nanoTime();
            int[] counts = dao.batchInsert(titles);
            long millis = (System.nanoTime() - start) / 1_000_000;
            long commands = comInsert(url, user, password) - before;
            long positive = java.util.Arrays.stream(counts).filter(c -> c > 0).count();
            long noInfo = java.util.Arrays.stream(counts).filter(c -> c == Statement.SUCCESS_NO_INFO).count();
            return label + ": " + dao.countAll() + " rows, INSERT commands received by MySQL " + commands + ", counts array: " + positive + " of 1, " + noInfo + " of -2 (success, no count)" + ", timing " + millis + " ms";
        }
    }

    private static long comInsert(String url, String user, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password);
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("show global status like 'Com_insert'")) {
            rs.next();
            return rs.getLong(2);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
