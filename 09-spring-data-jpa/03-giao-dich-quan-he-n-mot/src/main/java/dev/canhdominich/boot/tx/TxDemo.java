package dev.canhdominich.boot.tx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.UnexpectedRollbackException;

public class TxDemo {

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
        properties.put("server.port", "0");
        properties.put("spring.main.banner-mode", "off");
        properties.put("logging.level.root", "OFF");
        return properties;
    }

    public static List<String> run(String url, String user, String password) {
        List<String> lines = new ArrayList<>();
        SpringApplication application = new SpringApplication(TxApplication.class);
        application.setDefaultProperties(properties(url, user, password));
        int port;
        try (ConfigurableApplicationContext context = application.run()) {
            port = ((WebServerApplicationContext) context).getWebServer().getPort();
            AccountRepository accounts = context.getBean(AccountRepository.class);
            AuditRepository audits = context.getBean(AuditRepository.class);
            TransferService transfers = context.getBean(TransferService.class);
            CourseRepository courses = context.getBean(CourseRepository.class);
            CourseService courseService = context.getBean(CourseService.class);
            long id = accounts.save(new Account("An", 100)).getId();

            lines.add("-- which exceptions roll a transaction back");
            lines.add("unchecked exception after the debit: " + attempt(() -> transfers.debitThenFailUnchecked(id, 30), accounts, id));
            lines.add("checked exception after the debit: " + attempt(() -> sneaky(() -> transfers.debitThenFailChecked(id, 30)), accounts, id));
            reset(accounts, id);
            lines.add("checked exception with rollbackFor = Exception.class: " + attempt(() -> sneaky(() -> transfers.debitThenFailCheckedWithRollbackFor(id, 30)), accounts, id));

            lines.add("-- propagation");
            audits.deleteAll();
            reset(accounts, id);
            attempt(() -> transfers.debitAuditJoiningThenFail(id, 30), accounts, id);
            lines.add("audit joins the transaction, caller fails: balance " + accounts.findById(id).orElseThrow().getBalance() + ", audit rows " + audits.count());
            audits.deleteAll();
            reset(accounts, id);
            attempt(() -> transfers.debitAuditInNewTransactionThenFail(id, 30), accounts, id);
            lines.add("audit in REQUIRES_NEW, caller fails: balance " + accounts.findById(id).orElseThrow().getBalance() + ", audit rows " + audits.count());

            lines.add("-- the proxy and self-invocation");
            lines.add("REQUIRES_NEW method called with this.: transaction active = " + transfers.callsOwnTransactionalMethod());
            lines.add("REQUIRES_NEW method called through another bean: transaction active = " + transfers.callsAnotherBeansTransactionalMethod());

            lines.add("-- readOnly");
            reset(accounts, id);
            SqlRecorder.clear();
            transfers.changeInsideReadOnly(id);
            lines.add("change inside readOnly = true: balance " + accounts.findById(id).orElseThrow().getBalance() + ", update statements " + SqlRecorder.statements().stream().filter(sql -> sql.startsWith("update")).count());

            lines.add("-- a failure inside a joined transaction, swallowed by the caller");
            try {
                transfers.swallowFailureOfInnerTransaction();
                lines.add("outer method returned normally");
            } catch (UnexpectedRollbackException e) {
                lines.add("outer commit -> UnexpectedRollbackException: " + e.getMessage().split(";")[0]);
            }

            lines.add("-- relations and N+1 through repositories");
            for (int i = 1; i <= 10; i++) {
                Course course = new Course("Course " + i);
                for (int l = 1; l <= 3; l++) {
                    course.addLesson("Lesson " + i + "." + l);
                }
                courses.save(course);
            }
            lines.add(count("findAll then lessons inside a transaction", () -> courseService.lessonsPerCourseLazy()));
            lines.add(count("@EntityGraph(attributePaths = \"lessons\")", () -> courseService.lessonsPerCourseWithGraph()));
            lines.add(count("join fetch in @Query", () -> courseService.lessonsPerCourseJoinFetch()));
            try {
                courseService.lessonsWithoutTransaction();
            } catch (RuntimeException e) {
                lines.add("lazy collection with no transaction and no web request -> " + e.getClass().getSimpleName());
            }
            lines.add("-- open-in-view");
            lines.add("web request, no transaction, open-in-view default: " + request(port, "/lessons/count"));
        }
        SpringApplication second = new SpringApplication(TxApplication.class);
        Map<String, Object> off = properties(url, user, password);
        off.put("spring.jpa.open-in-view", "false");
        second.setDefaultProperties(off);
        try (ConfigurableApplicationContext context = second.run()) {
            context.getBean(CourseRepository.class).save(seedOne());
            int port2 = ((WebServerApplicationContext) context).getWebServer().getPort();
            lines.add("web request, no transaction, spring.jpa.open-in-view=false: " + request(port2, "/lessons/count"));
        }
        lines.forEach(System.out::println);
        return lines;
    }

    private static Course seedOne() {
        Course course = new Course("Only course");
        course.addLesson("Only lesson");
        return course;
    }

    private static String request(int port, String path) {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body().replaceAll("\\s+", " ");
            return response.statusCode() + (response.statusCode() == 200 ? " " + body : "");
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private static String count(String label, Supplier<Integer> action) {
        SqlRecorder.clear();
        int lessons = action.get();
        int statements = SqlRecorder.statements().size();
        SqlRecorder.clear();
        return label + " -> " + lessons + " lessons, " + statements + " statements";
    }

    private static void reset(AccountRepository accounts, long id) {
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(100);
        accounts.save(account);
    }

    private static String attempt(Runnable action, AccountRepository accounts, long id) {
        String outcome;
        try {
            action.run();
            outcome = "no exception";
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() instanceof IOException ? e.getCause() : e;
            outcome = cause.getClass().getSimpleName();
        }
        return outcome + ", balance after = " + accounts.findById(id).orElseThrow().getBalance();
    }

    @FunctionalInterface
    interface ThrowingRunnable {

        void run() throws Exception;
    }

    private static void sneaky(ThrowingRunnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
