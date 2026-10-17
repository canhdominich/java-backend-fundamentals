package dev.canhdominich.shop.auth;

import dev.canhdominich.shop.error.ConflictException;
import dev.canhdominich.shop.user.User;
import dev.canhdominich.shop.user.UserRepository;
import dev.canhdominich.shop.user.UserRole;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final String unknownUserHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.unknownUserHash = encoder.encode("password-of-a-user-that-does-not-exist");
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("EMAIL_TAKEN", "this email is already registered");
        }
        users.save(new User(email, encoder.encode(request.password()), UserRole.CUSTOMER));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email().trim().toLowerCase()).orElse(null);
        String hash = user == null ? unknownUserHash : user.getPasswordHash();
        boolean matches = encoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new BadCredentialsException("invalid email or password");
        }
        return tokens.issue(user);
    }
}
