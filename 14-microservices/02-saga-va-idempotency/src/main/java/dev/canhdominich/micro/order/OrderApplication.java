package dev.canhdominich.micro.order;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class OrderApplication {

    @Bean
    ApplicationRunner schema(JdbcTemplate jdbc) {
        return args -> jdbc.execute("create table orders (id varchar(40) primary key, sku varchar(20) not null, quantity int not null)");
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
