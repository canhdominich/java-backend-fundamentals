package dev.canhdominich.junit.orders;

public final class TaxTable {

    public int percentFor(String country) {
        return "VN".equals(country) ? 10 : 20;
    }
}
