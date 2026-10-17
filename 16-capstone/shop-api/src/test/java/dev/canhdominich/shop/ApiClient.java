package dev.canhdominich.shop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class ApiClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mvc;

    public ApiClient(MockMvc mvc) {
        this.mvc = mvc;
    }

    public String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@shop.test";
    }

    public String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"password-1\"}"));
        return login(email, "password-1");
    }

    public String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    public String adminToken() throws Exception {
        return login("admin@shop.test", "admin-password");
    }

    public long createProduct(String adminToken, String sku, long priceCents, int stock) throws Exception {
        MvcResult result = mvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"" + sku + "\",\"name\":\"Product " + sku + "\",\"priceCents\":" + priceCents + ",\"stock\":" + stock + "}")).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    public int stockOf(long productId) throws Exception {
        MvcResult result = mvc.perform(get("/api/products/" + productId)).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("stock").asInt();
    }

    public MockHttpServletRequestBuilder order(String token, String key, long productId, int quantity) {
        MockHttpServletRequestBuilder request = post("/api/orders").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return request;
    }

    public JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }
}
