package dev.canhdominich.micro.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class OrderController {

    private final JdbcTemplate jdbc;
    private final RestClient inventory;
    private final String inventoryUrl;

    public OrderController(JdbcTemplate jdbc, RestClient.Builder builder, @Value("${inventory.url}") String inventoryUrl) {
        this.jdbc = jdbc;
        this.inventoryUrl = inventoryUrl;
        this.inventory = builder.baseUrl(inventoryUrl).build();
    }

    @PostMapping("/orders")
    public String order(@RequestParam String sku, @RequestParam int quantity) {
        inventory.post().uri("/reserve?sku={sku}&quantity={quantity}", sku, quantity).retrieve().toBodilessEntity();
        jdbc.update("insert into orders (sku, quantity) values (?, ?)", sku, quantity);
        return "ordered";
    }

    @GetMapping("/orders/count")
    public String count() {
        return String.valueOf(jdbc.queryForObject("select count(*) from orders", Integer.class));
    }

    @GetMapping("/where-is-inventory")
    public String where() {
        return inventoryUrl;
    }
}
