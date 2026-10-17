package dev.canhdominich.boot.security;

import java.security.Principal;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Endpoints {

    @GetMapping("/public/info")
    public Map<String, String> info() {
        return Map.of("info", "anyone can read this");
    }

    @GetMapping("/api/me")
    public Map<String, String> me(Principal principal) {
        return Map.of("user", principal.getName());
    }

    @PostMapping("/api/orders")
    public Map<String, String> create() {
        return Map.of("created", "yes");
    }

    @GetMapping("/admin/stats")
    public Map<String, String> stats() {
        return Map.of("stats", "secret numbers");
    }

    @GetMapping("/api/accounts/{owner}")
    @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")
    public Map<String, String> account(@PathVariable String owner) {
        return Map.of("account", owner);
    }
}
