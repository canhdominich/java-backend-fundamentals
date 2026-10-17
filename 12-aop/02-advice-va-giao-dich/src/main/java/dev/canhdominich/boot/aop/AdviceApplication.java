package dev.canhdominich.boot.aop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement(order = 200)
public class AdviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdviceApplication.class, args);
    }
}
