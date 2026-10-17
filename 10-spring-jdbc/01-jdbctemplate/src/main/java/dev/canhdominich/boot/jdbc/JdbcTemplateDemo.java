package dev.canhdominich.boot.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcTemplateDemo {

    public static void main(String[] args) {
        run(env("JDBC_URL", "jdbc:mysql://localhost:3307/shop"), env("DB_USER", "app"), env("DB_PASSWORD", "app_pw"));
    }

    public static List<String> run(String url, String user, String password) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", user);
        properties.put("spring.datasource.password", password);
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "OFF");
        SpringApplication application = new SpringApplication(JdbcApplication.class);
        application.setDefaultProperties(properties);
        List<String> lines = new ArrayList<>();
        try (ConfigurableApplicationContext context = application.run()) {
            StudentDao dao = context.getBean(StudentDao.class);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            RegistrationService registration = context.getBean(RegistrationService.class);
            HikariDataSource pool = (HikariDataSource) context.getBean(DataSource.class);
            dao.createTable();
            lines.add("-- writing and reading");
            long an = dao.insert("An", "an@example.com");
            long binh = dao.insert("Binh", "binh@example.com");
            lines.add("generated keys: " + an + ", " + binh);
            lines.add("findById(" + an + ") -> " + dao.findById(an));
            lines.add("findAll -> " + dao.findAll().size() + " students");
            lines.add("rename existing id -> rows updated " + dao.rename(an, "An Nguyen"));
            lines.add("rename missing id -> rows updated " + dao.rename(9999, "Nobody"));
            lines.add("count -> " + dao.count());
            lines.add("-- connections: borrowed and returned for you");
            lines.add("active connections after the calls above: " + pool.getHikariPoolMXBean().getActiveConnections());
            lines.add("-- exceptions with a clear meaning");
            lines.add("queryForObject, no row -> " + describe(() -> dao.findById(9999)));
            lines.add("findByEmail wrapper for the same case -> " + dao.findByEmail("nobody@example.com"));
            lines.add("queryForObject, two rows -> " + describe(() -> jdbc.queryForObject("select name from student where name like ?", String.class, "%n%")));
            lines.add("duplicate email -> " + describe(() -> dao.insert("Another", "an@example.com")));
            lines.add("bad SQL -> " + describe(() -> jdbc.queryForObject("select nope from student", Integer.class)));
            jdbc.setQueryTimeout(1);
            lines.add("query timeout of 1 s on select sleep(3) -> " + describe(() -> jdbc.queryForObject("select sleep(3)", Integer.class)));
            jdbc.setQueryTimeout(-1);
            lines.add("-- transactions");
            int before = dao.count();
            lines.add("without @Transactional, failure after one insert: " + describe(() -> registration.registerTwoWithoutTransaction("c1", "c2")) + "; rows added " + (dao.count() - before));
            int middle = dao.count();
            lines.add("with @Transactional, same failure: " + describe(() -> registration.registerTwoInTransaction("d1", "d2")) + "; rows added " + (dao.count() - middle));
            lines.add("active connections at the end: " + pool.getHikariPoolMXBean().getActiveConnections());
        }
        lines.forEach(System.out::println);
        return lines;
    }

    private static String describe(Runnable action) {
        try {
            action.run();
            return "no exception";
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String message = e.getMessage() == null ? "" : e.getMessage().split("\n")[0];
            if (message.length() > 110) {
                message = message.substring(0, 110) + "...";
            }
            return e.getClass().getSimpleName() + " (root cause " + root.getClass().getSimpleName() + ")" + (message.isEmpty() ? "" : ": " + message);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
