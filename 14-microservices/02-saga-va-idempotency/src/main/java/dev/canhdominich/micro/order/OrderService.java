package dev.canhdominich.micro.order;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class OrderService {

    private static final int ATTEMPTS = 3;

    private final JdbcTemplate jdbc;
    private final RestClient inventory;
    private final List<String> pendingCompensations = new ArrayList<>();

    public OrderService(JdbcTemplate jdbc, RestClient.Builder builder, @Value("${inventory.url}") String inventoryUrl) {
        this.jdbc = jdbc;
        this.inventory = builder.baseUrl(inventoryUrl).build();
    }

    public String place(String orderId, String sku, int quantity, boolean safe, boolean failAfterReserve) {
        String key = safe ? orderId : null;
        try {
            reserveWithRetry(sku, quantity, key);
            if (failAfterReserve) {
                throw new IllegalStateException("simulated failure after the stock was reserved");
            }
            jdbc.update("insert into orders values (?, ?, ?)", orderId, sku, quantity);
            return "ordered";
        } catch (RuntimeException e) {
            if (safe && !release(orderId)) {
                pendingCompensations.add(orderId);
            }
            throw e;
        }
    }

    public int retryPendingCompensations() {
        int done = 0;
        for (Iterator<String> it = pendingCompensations.iterator(); it.hasNext(); ) {
            if (release(it.next())) {
                it.remove();
                done++;
            }
        }
        return done;
    }

    public int pending() {
        return pendingCompensations.size();
    }

    public int orderCount() {
        return jdbc.queryForObject("select count(*) from orders", Integer.class);
    }

    private void reserveWithRetry(String sku, int quantity, String key) {
        for (int attempt = 1; ; attempt++) {
            try {
                inventory.post().uri("/reserve?sku={sku}&quantity={quantity}" + (key == null ? "" : "&key={key}"), key == null ? new Object[] {sku, quantity} : new Object[] {sku, quantity, key})
                        .retrieve().toBodilessEntity();
                return;
            } catch (ResourceAccessException e) {
                if (attempt == ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private boolean release(String key) {
        try {
            inventory.post().uri("/release?key={key}", key).retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
