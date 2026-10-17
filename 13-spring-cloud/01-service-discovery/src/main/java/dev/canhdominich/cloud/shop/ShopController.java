package dev.canhdominich.cloud.shop;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class ShopController {

    private final RestClient catalog;
    private final DiscoveryClient discovery;

    public ShopController(@LoadBalanced RestClient.Builder builder, DiscoveryClient discovery) {
        this.catalog = builder.baseUrl("http://catalog").build();
        this.discovery = discovery;
    }

    @GetMapping("/call")
    public String call() {
        return catalog.get().uri("/whoami").retrieve().body(String.class);
    }

    @GetMapping("/instances")
    public String instances() {
        return String.valueOf(discovery.getInstances("catalog").size());
    }
}
