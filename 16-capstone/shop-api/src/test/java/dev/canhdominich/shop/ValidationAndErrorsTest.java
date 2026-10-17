package dev.canhdominich.shop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ValidationAndErrorsTest {

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
    void anInvalidProductListsEveryBadField() throws Exception {
        String admin = api.adminToken();
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"\",\"priceCents\":-1,\"stock\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void aDuplicateSkuIsAConflict() throws Exception {
        String admin = api.adminToken();
        api.createProduct(admin, "DUP-1", 100, 1);
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"DUP-1\",\"name\":\"again\",\"priceCents\":1,\"stock\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));
    }

    @Test
    void anUnknownProductIs404() throws Exception {
        mvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void orderingMoreThanTheStockIsAConflictAndChangesNothing() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "SHORT-1", 100, 2);
        String customer = api.registerAndLogin(api.uniqueEmail());
        mvc.perform(api.order(customer, null, productId, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
        org.assertj.core.api.Assertions.assertThat(api.stockOf(productId)).isEqualTo(2);
    }

    @Test
    void anOrderThatFailsHalfWayRollsBackTheLinesAlreadyReserved() throws Exception {
        String admin = api.adminToken();
        long plenty = api.createProduct(admin, "HALF-1", 100, 10);
        long scarce = api.createProduct(admin, "HALF-2", 100, 1);
        String customer = api.registerAndLogin(api.uniqueEmail());
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":" + plenty + ",\"quantity\":4},{\"productId\":" + scarce + ",\"quantity\":5}]}"))
                .andExpect(status().isConflict());
        org.assertj.core.api.Assertions.assertThat(api.stockOf(plenty)).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(api.stockOf(scarce)).isEqualTo(1);
    }

    @Test
    void anEmptyOrderAndAnOversizedQuantityAreRejected() throws Exception {
        String customer = api.registerAndLogin(api.uniqueEmail());
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customer).contentType(MediaType.APPLICATION_JSON).content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":1,\"quantity\":1001}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aDeactivatedProductDisappearsFromTheCatalog() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "GONE-1", 100, 1);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/products/" + productId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/products/" + productId)).andExpect(status().isNotFound());
    }

    @Test
    void updatingAProductChangesItsVersion() throws Exception {
        String admin = api.adminToken();
        long productId = api.createProduct(admin, "VER-1", 100, 1);
        mvc.perform(put("/api/products/" + productId).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\",\"priceCents\":200,\"stock\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("renamed"))
                .andExpect(jsonPath("$.version").value(1));
    }
}
