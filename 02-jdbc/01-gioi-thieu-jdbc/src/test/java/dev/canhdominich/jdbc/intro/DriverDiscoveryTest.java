package dev.canhdominich.jdbc.intro;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DriverDiscoveryTest {

    @Test
    void bothDriversAreRegisteredWithoutClassForName() {
        var found = ServiceLoader.load(Driver.class).stream()
                .map(provider -> provider.type().getName())
                .collect(Collectors.toSet());
        assertThat(found).contains("com.mysql.cj.jdbc.Driver", "org.h2.Driver");
        assertThat(DriverManager.drivers().map(driver -> driver.getClass().getName()))
                .contains("com.mysql.cj.jdbc.Driver", "org.h2.Driver");
    }

    @Test
    void driverIsChosenByUrlPrefix() {
        assertThat(DriverDiscovery.describe("jdbc:mysql://localhost:3307/shop")).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(DriverDiscovery.describe("jdbc:h2:mem:lab")).isEqualTo("org.h2.Driver");
    }

    @Test
    void unknownPrefixGivesNoSuitableDriverWithSqlState08001() {
        assertThat(DriverDiscovery.describe("jdbc:postgresql://localhost:5432/shop"))
                .contains("SQLState=08001")
                .contains("No suitable driver");
        assertThat(DriverDiscovery.describe("mysql://localhost:3307/shop")).contains("SQLState=08001");
    }
}
