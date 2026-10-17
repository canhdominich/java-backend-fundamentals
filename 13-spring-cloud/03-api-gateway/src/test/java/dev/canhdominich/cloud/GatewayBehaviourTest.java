package dev.canhdominich.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GatewayBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runScenario() throws Exception {
        lines = GatewayDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void routeStripsPrefixAndAddsHeaders() {
        assertThat(line("GET /api/catalog/products:")).endsWith("200 products from catalog-1");
        assertThat(line("what the backend received")).endsWith("/echo | X-Gateway=shop-edge");
        assertThat(line("response header added")).endsWith("X-Served-By=gateway");
    }

    @Test
    void rewritePathReachesTheRightEndpoint() {
        assertThat(line("GET /api/orders/5")).endsWith("200 order 5");
    }

    @Test
    void unknownPathHasNoRoute() {
        assertThat(line("GET /nothing")).endsWith("status 404");
    }

    @Test
    void loadBalancedRouteAlternates() {
        assertThat(line("6 calls")).endsWith("{products from catalog-1=3, products from catalog-2=3}");
    }

    @Test
    void edgeFilterRejectsBeforeTheBackendIsReached() {
        assertThat(line("GET /api/catalog/products without key")).endsWith("status 401");
        assertThat(line("GET /api/catalog/products with wrong key")).endsWith("status 401");
        assertThat(line("backend hits changed")).endsWith(": 0");
        assertThat(line("GET /api/catalog/products with key")).endsWith("status 200");
        assertThat(line("calling a backend directly")).endsWith("status 200");
    }

    @Test
    void stoppedBackendGivesServerError() {
        assertThat(line("GET /api/orders/5 after stopping")).endsWith("status 500");
    }
}
