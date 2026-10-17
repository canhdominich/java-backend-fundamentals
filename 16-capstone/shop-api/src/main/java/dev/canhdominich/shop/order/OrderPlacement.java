package dev.canhdominich.shop.order;

import dev.canhdominich.shop.error.ConflictException;
import dev.canhdominich.shop.error.NotFoundException;
import dev.canhdominich.shop.product.Product;
import dev.canhdominich.shop.product.ProductRepository;
import dev.canhdominich.shop.user.User;
import dev.canhdominich.shop.user.UserRepository;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OrderPlacement {

    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;

    OrderPlacement(UserRepository users, ProductRepository products, OrderRepository orders) {
        this.users = users;
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    Order execute(long userId, String idempotencyKey, OrderRequest request) {
        User user = users.getReferenceById(userId);
        Map<Long, Integer> wanted = new TreeMap<>();
        for (OrderRequest.Line line : request.lines()) {
            wanted.merge(line.productId(), line.quantity(), Integer::sum);
        }
        Order order = new Order(user, idempotencyKey);
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = products.findByIdAndActiveTrue(entry.getKey())
                    .orElseThrow(() -> new NotFoundException("product " + entry.getKey() + " not found"));
            if (products.decrementStock(product.getId(), entry.getValue()) == 0) {
                throw new ConflictException("OUT_OF_STOCK", "not enough stock for " + product.getSku());
            }
            order.addLine(new OrderLine(order, product, entry.getValue(), product.getPriceCents()));
        }
        return orders.saveAndFlush(order);
    }

    @Transactional
    Order cancel(long orderId) {
        Order order = orders.findWithLinesById(orderId).orElseThrow(() -> new NotFoundException("order " + orderId + " not found"));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ConflictException("NOT_CANCELLABLE", "order " + orderId + " is " + order.getStatus());
        }
        order.cancel();
        for (OrderLine line : order.getLines()) {
            products.incrementStock(line.getProduct().getId(), line.getQuantity());
        }
        return order;
    }
}
