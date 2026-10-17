package dev.canhdominich.hibernate.crud;

public record DbConfig(String url, String user, String password) {

    public static DbConfig fromEnvironment() {
        return new DbConfig(
                env("JDBC_URL", "jdbc:mysql://localhost:3307/shop"),
                env("DB_USER", "app"),
                env("DB_PASSWORD", "app_pw"));
    }

    public DbConfig withUrl(String newUrl) {
        return new DbConfig(newUrl, user, password);
    }

    public DbConfig withPassword(String newPassword) {
        return new DbConfig(url, user, newPassword);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
