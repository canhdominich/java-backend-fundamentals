package dev.canhdominich.shop.bootstrap;

import dev.canhdominich.shop.user.User;
import dev.canhdominich.shop.user.UserRepository;
import dev.canhdominich.shop.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository users, PasswordEncoder encoder,
                            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (!users.existsByEmail(normalized)) {
            users.save(new User(normalized, encoder.encode(password), UserRole.ADMIN));
            LOG.info("created the initial admin account {}", normalized);
        }
    }
}
