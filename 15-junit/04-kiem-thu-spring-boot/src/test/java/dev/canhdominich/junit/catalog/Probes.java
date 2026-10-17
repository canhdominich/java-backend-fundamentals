package dev.canhdominich.junit.catalog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

public final class Probes {

    static final List<String> LOG = new ArrayList<>();

    private Probes() {
    }

    static String describe(ApplicationContext context) {
        return "context " + Integer.toHexString(System.identityHashCode(context))
                + ", beans " + context.getBeanDefinitionCount()
                + ", has controller " + (context.getBeanNamesForType(ProductController.class).length > 0)
                + ", has repository " + (context.getBeanNamesForType(ProductRepository.class).length > 0);
    }

    @SpringBootTest
    public static class FullContextA {

        @Autowired
        ApplicationContext context;

        @Test
        void remember() {
            LOG.add(describe(context));
        }
    }

    @SpringBootTest
    public static class FullContextB {

        @Autowired
        ApplicationContext context;

        @Test
        void remember() {
            LOG.add(describe(context));
        }
    }

    @SpringBootTest
    public static class FullContextWithAMock {

        @Autowired
        ApplicationContext context;

        @MockitoBean
        ProductService service;

        @Test
        void remember() {
            LOG.add(describe(context));
        }
    }

    @WebMvcTest(ProductController.class)
    public static class WebSlice {

        @Autowired
        ApplicationContext context;

        @MockitoBean
        ProductService service;

        @Test
        void remember() {
            LOG.add(describe(context));
        }
    }

    @DataJpaTest
    public static class JpaSlice {

        @Autowired
        ApplicationContext context;

        @Test
        void remember() {
            LOG.add(describe(context));
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class RollbackWithMockMvc {

        @Autowired
        MockMvc mvc;

        @Autowired
        ProductRepository products;

        @Test
        @Order(1)
        void createsAProduct() throws Exception {
            mvc.perform(MockMvcRequestBuilders.post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Book\", \"price\": 15000}"));
            LOG.add("inside the test: " + products.count());
        }

        @Test
        @Order(2)
        void countsAgain() {
            LOG.add("in the next test: " + products.count());
        }
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Transactional
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public static class RollbackWithARealServer {

        @LocalServerPort
        int port;

        @Autowired
        ProductRepository products;

        @Test
        @Order(1)
        void createsAProduct() throws Exception {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/products"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"name\": \"Book\", \"price\": 15000}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            LOG.add("HTTP status " + response.statusCode() + ", inside the test: " + products.count());
        }

        @Test
        @Order(2)
        void countsAgain() {
            LOG.add("in the next test: " + products.count());
        }
    }
}
