package dev.canhdominich.micro.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final JdbcTemplate jdbc;

    public StockService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String reserve(String sku, int quantity, String key) {
        if (key != null) {
            Integer known = jdbc.queryForObject("select count(*) from reservations where reservation_key = ?", Integer.class, key);
            if (known != null && known > 0) {
                return "already reserved";
            }
        }
        int updated = jdbc.update("update stock set quantity = quantity - ? where sku = ? and quantity >= ?", quantity, sku, quantity);
        if (updated == 0) {
            throw new IllegalStateException("out of stock");
        }
        if (key != null) {
            jdbc.update("insert into reservations values (?, ?, ?, false)", key, sku, quantity);
        }
        return "reserved";
    }

    @Transactional
    public String release(String key) {
        var rows = jdbc.queryForList("select sku, quantity, released from reservations where reservation_key = ?", key);
        if (rows.isEmpty()) {
            return "nothing to release";
        }
        var row = rows.get(0);
        if ((Boolean) row.get("RELEASED")) {
            return "already released";
        }
        jdbc.update("update stock set quantity = quantity + ? where sku = ?", row.get("QUANTITY"), row.get("SKU"));
        jdbc.update("update reservations set released = true where reservation_key = ?", key);
        return "released";
    }

    public int stock(String sku) {
        return jdbc.queryForObject("select quantity from stock where sku = ?", Integer.class, sku);
    }
}
