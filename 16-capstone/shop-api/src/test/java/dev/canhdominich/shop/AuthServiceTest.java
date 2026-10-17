package dev.canhdominich.shop;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.canhdominich.shop.auth.AuthService;
import dev.canhdominich.shop.auth.LoginRequest;
import dev.canhdominich.shop.auth.TokenService;
import dev.canhdominich.shop.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @Test
    void anUnknownEmailStillPaysForAHashComparison() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("nobody@shop.test")).thenReturn(Optional.empty());
        PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));
        AuthService auth = new AuthService(users, encoder, mock(TokenService.class));

        assertThatThrownBy(() -> auth.login(new LoginRequest("nobody@shop.test", "password-123")))
                .isInstanceOf(BadCredentialsException.class);

        verify(encoder).matches(eq("password-123"), anyString());
    }
}
