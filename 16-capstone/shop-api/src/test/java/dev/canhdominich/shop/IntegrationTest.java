package dev.canhdominich.shop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-that-is-long-enough-for-hs256-signing",
        "app.admin.email=admin@shop.test",
        "app.admin.password=admin-password"
})
@AutoConfigureMockMvc
public @interface IntegrationTest {
}
