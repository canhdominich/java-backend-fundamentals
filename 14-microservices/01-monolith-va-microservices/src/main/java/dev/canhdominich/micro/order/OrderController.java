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

    public OrderController(JdbcTemplate jdbc, RestClient.Builder builder, @Value("${inventory.url}") String inventoryUrl) {
        this.jdbc = jdbc;
        this.inventory = builder.baseUrl(inventoryUrl).build();
    }

    @PostMapping("/orders")
    public String order(@RequestParam String sku, @RequestParam int quantity, @RequestParam(defaultValue = "false") boolean failAfterReserve) {
        inventory.post().uri("/reserve?sku={sku}&quantity={quantity}", sku, quantity).retrieve().toBodilessEntity();
        if (failAfterReserve) {
            throw new IllegalStateException("simulated failure after the stock was reserved");
        }
        jdbc.update("insert into orders (sku, quantity) values (?, ?)", sku, quantity);
        return "ordered";
    }

    @GetMapping("/orders/count")
    public String count() {
        return String.valueOf(jdbc.queryForObject("select count(*) from orders", Integer.class));
    }
}
