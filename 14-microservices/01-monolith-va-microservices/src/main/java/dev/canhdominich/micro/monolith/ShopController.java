package dev.canhdominich.micro.monolith;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShopController {

    private final ShopService shop;

    public ShopController(ShopService shop) {
        this.shop = shop;
    }

    @PostMapping("/orders")
    public String order(@RequestParam String sku, @RequestParam int quantity, @RequestParam(defaultValue = "false") boolean failAfterReserve) {
        shop.placeOrder(sku, quantity, failAfterReserve);
        return "ordered";
    }

    @GetMapping("/stock/{sku}")
    public String stock(@PathVariable String sku) {
        return String.valueOf(shop.stock(sku));
    }

    @GetMapping("/orders/count")
    public String count() {
        return String.valueOf(shop.orderCount());
    }
}
