package dev.canhdominich.jdbc.intro;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ServiceLoader;

public final class DriverDiscovery {

    private static final String[] URLS = {
            "jdbc:mysql://localhost:3307/shop",
            "jdbc:h2:mem:lab",
            "jdbc:postgresql://localhost:5432/shop",
            "mysql://localhost:3307/shop"
    };

    private DriverDiscovery() {
    }

    public static void main(String[] args) {
        System.out.println("ServiceLoader finds:");
        for (Driver driver : ServiceLoader.load(Driver.class)) {
            System.out.println("  " + driver.getClass().getName());
        }
        System.out.println("DriverManager.getDrivers():");
        DriverManager.drivers().forEach(driver -> System.out.println("  " + driver.getClass().getName()));
        for (String url : URLS) {
            System.out.println("getDriver(" + url + ")");
            System.out.println("  " + describe(url));
        }
    }

    static String describe(String url) {
        try {
            return DriverManager.getDriver(url).getClass().getName();
        } catch (SQLException e) {
            return e.getClass().getSimpleName() + " SQLState=" + e.getSQLState() + " message=" + e.getMessage();
        }
    }
}
