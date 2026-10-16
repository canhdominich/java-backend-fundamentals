package dev.canhdominich.hibernate.intro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

public class SqlRecorder implements StatementInspector {

    private static final List<String> STATEMENTS = Collections.synchronizedList(new ArrayList<>());

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql.replaceAll("\\s+", " ").trim());
        return sql;
    }

    public static void clear() {
        STATEMENTS.clear();
    }

    public static List<String> statements() {
        synchronized (STATEMENTS) {
            return new ArrayList<>(STATEMENTS);
        }
    }

    public static void print(String title) {
        System.out.println(title);
        List<String> list = statements();
        if (list.isEmpty()) {
            System.out.println("  (no SQL)");
        }
        list.forEach(sql -> System.out.println("  " + sql));
        clear();
    }
}
