package dev.canhdominich.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class IdempotencyTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        MySqlContainer.register(registry);
    }

    @Autowired
    MockMvc mvc;

    ApiClient api;

    @BeforeEach
    void createClient() {
        api = new ApiClient(mvc);
    }

    @Test
    void repeatingARequestWithTheSameKeyReturnsTheSameOrderAndChargesOnce() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "IDEM-1", 100_000, 10);
        String customer = api.registerAndLogin(api.uniqueEmail());

        MvcResult first = mvc.perform(api.order(customer, "key-1", productId, 2)).andExpect(status().isCreated()).andExpect(header().string("Idempotent-Replayed", "false")).andReturn();
        MvcResult second = mvc.perform(api.order(customer, "key-1", productId, 2)).andExpect(status().isOk()).andExpect(header().string("Idempotent-Replayed", "true")).andReturn();

        assertThat(api.json(second).get("id").asLong()).isEqualTo(api.json(first).get("id").asLong());
        assertThat(api.stockOf(productId)).isEqualTo(8);
    }

    @Test
    void theSameKeyWithADifferentBodyStillReturnsTheFirstOrder() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "IDEM-4", 100_000, 10);
        String customer = api.registerAndLogin(api.uniqueEmail());

        MvcResult first = mvc.perform(api.order(customer, "key-x", productId, 1)).andExpect(status().isCreated()).andReturn();
        MvcResult second = mvc.perform(api.order(customer, "key-x", productId, 5)).andExpect(status().isOk()).andReturn();

        assertThat(api.json(second).get("id").asLong()).isEqualTo(api.json(first).get("id").asLong());
        assertThat(api.json(second).get("totalCents").asLong()).isEqualTo(100_000);
        assertThat(api.stockOf(productId)).isEqualTo(9);
    }

    @Test
    void withoutAKeyEveryRequestIsANewOrder() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "IDEM-2", 100_000, 10);
        String customer = api.registerAndLogin(api.uniqueEmail());

        mvc.perform(api.order(customer, null, productId, 1)).andExpect(status().isCreated());
        mvc.perform(api.order(customer, null, productId, 1)).andExpect(status().isCreated());

        assertThat(api.stockOf(productId)).isEqualTo(8);
    }

    @Test
    void theSameKeyFromAnotherCustomerIsADifferentOrder() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "IDEM-3", 100_000, 10);
        String first = api.registerAndLogin(api.uniqueEmail());
        String second = api.registerAndLogin(api.uniqueEmail());

        mvc.perform(api.order(first, "shared", productId, 1)).andExpect(status().isCreated());
        mvc.perform(api.order(second, "shared", productId, 1)).andExpect(status().isCreated());

        assertThat(api.stockOf(productId)).isEqualTo(8);
    }
}
