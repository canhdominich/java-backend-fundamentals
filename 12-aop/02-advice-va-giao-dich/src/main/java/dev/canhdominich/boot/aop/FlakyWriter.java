package dev.canhdominich.boot.aop;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlakyWriter {

    private final JdbcTemplate jdbc;
    private final AtomicInteger outsideCalls = new AtomicInteger();
    private final AtomicInteger insideCalls = new AtomicInteger();

    public FlakyWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @RetryOutside
    @Transactional
    public String saveWithRetryOutsideTransaction(String name) {
        jdbc.update("insert into saved (name) values (?)", name);
        if (outsideCalls.incrementAndGet() <= 2) {
            throw new IllegalStateException("flaky failure " + outsideCalls.get());
        }
        return "saved";
    }

    @RetryInside
    @Transactional
    public String saveWithRetryInsideTransaction(String name) {
        jdbc.update("insert into saved (name) values (?)", name);
        if (insideCalls.incrementAndGet() <= 2) {
            throw new IllegalStateException("flaky failure " + insideCalls.get());
        }
        return "saved";
    }

    public int count(String name) {
        return jdbc.queryForObject("select count(*) from saved where name = ?", Integer.class, name);
    }
}
