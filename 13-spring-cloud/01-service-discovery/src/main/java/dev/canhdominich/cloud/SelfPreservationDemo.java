package dev.canhdominich.cloud;

import dev.canhdominich.cloud.DiscoveryDemo.Node;
import dev.canhdominich.cloud.catalog.CatalogApplication;
import dev.canhdominich.cloud.registry.RegistryApplication;
import java.util.ArrayList;
import java.util.List;

public class SelfPreservationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("self preservation false, crash 10 s after start: evicted after " + secondsUntilEvicted("false", 10) + " s");
        System.out.println("self preservation true, crash 10 s after start: evicted after " + secondsUntilEvicted("true", 10) + " s");
        System.out.println("self preservation true, crash 90 s after start: evicted after " + secondsUntilEvicted("true", 90) + " s");
    }

    static long secondsUntilEvicted(String selfPreservation, int crashAfterSeconds) throws Exception {
        List<Node> nodes = new ArrayList<>();
        try {
            Node registry = DiscoveryDemo.launch(nodes, RegistryApplication.class, "registry",
                    "--eureka.client.register-with-eureka=false",
                    "--eureka.client.fetch-registry=false",
                    "--eureka.server.enable-self-preservation=" + selfPreservation,
                    "--eureka.server.eviction-interval-timer-in-ms=1000",
                    "--eureka.server.response-cache-update-interval-ms=1000");
            long registryStarted = System.currentTimeMillis();
            String eureka = "http://localhost:" + registry.port() + "/eureka/";
            DiscoveryDemo.launch(nodes, CatalogApplication.class, "catalog", DiscoveryDemo.catalogArgs(eureka, "catalog-A"));
            Node b = DiscoveryDemo.launch(nodes, CatalogApplication.class, "catalog", DiscoveryDemo.catalogArgs(eureka, "catalog-B"));
            DiscoveryDemo.waitFor(() -> DiscoveryDemo.instanceIds(registry.port()).equals("[catalog-A, catalog-B]"), 30_000);
            Thread.sleep(Math.max(0, crashAfterSeconds * 1000L - (System.currentTimeMillis() - registryStarted)));
            b.process().destroyForcibly();
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 180_000) {
                if (DiscoveryDemo.instanceIds(registry.port()).equals("[catalog-A]")) {
                    return (System.currentTimeMillis() - start) / 1000;
                }
                Thread.sleep(1000);
            }
            return -1;
        } finally {
            for (Node node : nodes) {
                node.process().destroyForcibly();
            }
        }
    }
}
