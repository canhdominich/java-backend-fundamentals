package dev.canhdominich.boot.jwt;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authentication;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authentication, TokenService tokens) {
        this.authentication = authentication;
        this.tokens = tokens;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        try {
            Authentication result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.get("username"), body.get("password")));
            String token = tokens.issue(result.getName(), result.getAuthorities().stream().filter(a -> a.getAuthority().startsWith("ROLE_")).toList(), Duration.ofMinutes(15));
            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "bad credentials"));
        }
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(Principal principal, Authentication authentication) {
        return Map.of("user", principal.getName(), "authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }

    @GetMapping("/admin/stats")
    public Map<String, String> stats() {
        return Map.of("stats", "secret numbers");
    }
}
