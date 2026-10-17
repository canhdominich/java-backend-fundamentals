package dev.canhdominich.boot.security;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello(Principal principal) {
        return Map.of("message", "hello " + principal.getName());
    }

    @PostMapping("/orders")
    public Map<String, String> create() {
        return Map.of("created", "yes");
    }
}
