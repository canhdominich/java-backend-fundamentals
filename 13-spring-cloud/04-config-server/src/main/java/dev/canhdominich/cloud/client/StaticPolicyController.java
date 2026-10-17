package dev.canhdominich.cloud.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticPolicyController {

    @Value("${orders.max-items:unset}")
    private String maxItems;

    @GetMapping("/static-policy")
    public String policy() {
        return "maxItems=" + maxItems;
    }
}
