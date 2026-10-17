package dev.canhdominich.micro.monolith;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final JdbcTemplate jdbc;

    public ShopService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void placeOrder(String sku, int quantity, boolean failAfterReserve) {
        int updated = jdbc.update("update stock set quantity = quantity - ? where sku = ? and quantity >= ?", quantity, sku, quantity);
        if (updated == 0) {
            throw new IllegalStateException("out of stock");
        }
        jdbc.update("insert into orders (sku, quantity) values (?, ?)", sku, quantity);
        if (failAfterReserve) {
            throw new IllegalStateException("simulated failure after the stock was reserved");
        }
    }

    public int stock(String sku) {
        return jdbc.queryForObject("select quantity from stock where sku = ?", Integer.class, sku);
    }

    public int orderCount() {
        return jdbc.queryForObject("select count(*) from orders", Integer.class);
    }
}
