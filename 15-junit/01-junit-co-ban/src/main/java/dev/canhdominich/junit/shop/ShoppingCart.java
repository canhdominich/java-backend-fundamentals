package dev.canhdominich.junit.shop;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShoppingCart {

    private final Map<String, Line> lines = new LinkedHashMap<>();

    public void add(String sku, int quantity, long unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        lines.merge(sku, new Line(quantity, unitPrice), (old, added) -> new Line(old.quantity() + added.quantity(), added.unitPrice()));
    }

    public long total() {
        return lines.values().stream().mapToLong(line -> line.quantity() * line.unitPrice()).sum();
    }

    public int itemCount() {
        return lines.values().stream().mapToInt(Line::quantity).sum();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public void clear() {
        lines.clear();
    }

    private record Line(int quantity, long unitPrice) {
    }
}
