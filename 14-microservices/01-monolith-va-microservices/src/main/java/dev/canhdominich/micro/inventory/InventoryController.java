package dev.canhdominich.micro.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class InventoryController {

    private final JdbcTemplate jdbc;

    public InventoryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/reserve")
    public String reserve(@RequestParam String sku, @RequestParam int quantity) {
        int updated = jdbc.update("update stock set quantity = quantity - ? where sku = ? and quantity >= ?", quantity, sku, quantity);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "out of stock");
        }
        return "reserved";
    }

    @GetMapping("/stock/{sku}")
    public String stock(@PathVariable String sku) {
        return String.valueOf(jdbc.queryForObject("select quantity from stock where sku = ?", Integer.class, sku));
    }
}
