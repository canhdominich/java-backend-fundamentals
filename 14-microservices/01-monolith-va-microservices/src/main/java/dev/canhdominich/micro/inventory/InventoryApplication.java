package dev.canhdominich.micro.inventory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class InventoryApplication {

    @Bean
    ApplicationRunner schema(JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("create table stock (sku varchar(20) primary key, quantity int not null)");
            jdbc.update("insert into stock values ('BOOK', 10000)");
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
