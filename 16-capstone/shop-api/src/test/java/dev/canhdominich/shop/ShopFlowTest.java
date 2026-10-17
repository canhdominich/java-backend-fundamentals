package dev.canhdominich.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class ShopFlowTest {

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
    void aCustomerOrdersAProductAndTheStockGoesDown() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "FLOW-1", 1_500_000, 10);
        String customer = api.registerAndLogin(api.uniqueEmail());

        MvcResult created = mvc.perform(api.order(customer, null, productId, 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalCents").value(4_500_000))
                .andExpect(jsonPath("$.lines[0].sku").value("FLOW-1"))
                .andReturn();

        assertThat(api.stockOf(productId)).isEqualTo(7);
        long orderId = api.json(created).get("id").asLong();
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(1));
    }

    @Test
    void cancellingAnOrderReturnsTheStock() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "FLOW-2", 100_000, 5);
        String customer = api.registerAndLogin(api.uniqueEmail());
        long orderId = api.json(mvc.perform(api.order(customer, null, productId, 2)).andReturn()).get("id").asLong();

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(api.stockOf(productId)).isEqualTo(5);
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", "Bearer " + customer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_CANCELLABLE"));
        assertThat(api.stockOf(productId)).isEqualTo(5);
    }

    @Test
    void anotherCustomerCannotSeeMyOrderButAnAdminCan() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "FLOW-3", 100_000, 5);
        String owner = api.registerAndLogin(api.uniqueEmail());
        String stranger = api.registerAndLogin(api.uniqueEmail());
        long orderId = api.json(mvc.perform(api.order(owner, null, productId, 1)).andReturn()).get("id").asLong();

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
    }

    @Test
    void myOrderListShowsOnlyMyOrdersWithoutLines() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "FLOW-4", 100_000, 10);
        String me = api.registerAndLogin(api.uniqueEmail());
        String other = api.registerAndLogin(api.uniqueEmail());
        mvc.perform(api.order(me, null, productId, 1));
        mvc.perform(api.order(me, null, productId, 1));
        mvc.perform(api.order(other, null, productId, 1));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].lines.length()").value(0));
    }
}
