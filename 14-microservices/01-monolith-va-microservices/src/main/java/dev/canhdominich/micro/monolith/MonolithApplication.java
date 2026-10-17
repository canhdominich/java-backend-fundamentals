package dev.canhdominich.micro.monolith;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class MonolithApplication {

    @Bean
    ApplicationRunner schema(JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("create table stock (sku varchar(20) primary key, quantity int not null)");
            jdbc.execute("create table orders (id bigint auto_increment primary key, sku varchar(20) not null, quantity int not null)");
            jdbc.update("insert into stock values ('BOOK', 10000)");
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
