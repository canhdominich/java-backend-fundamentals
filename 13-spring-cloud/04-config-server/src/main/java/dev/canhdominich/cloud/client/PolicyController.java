package dev.canhdominich.cloud.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class PolicyController {

    @Value("${orders.max-items:unset}")
    private String maxItems;

    @Value("${orders.currency:unset}")
    private String currency;

    @GetMapping("/policy")
    public String policy() {
        return "maxItems=" + maxItems + " currency=" + currency;
    }
}
