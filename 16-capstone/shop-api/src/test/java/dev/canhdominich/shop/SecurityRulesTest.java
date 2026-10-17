package dev.canhdominich.shop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.canhdominich.shop.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SecurityRulesTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        MySqlContainer.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtEncoder encoder;

    ApiClient api;

    @BeforeEach
    void createClient() {
        api = new ApiClient(mvc);
    }

    @Test
    void theCatalogIsPublic() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void healthAndApiDocsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void ordersNeedAToken() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void aCustomerCannotChangeTheCatalog() throws Exception {
        String customer = api.registerAndLogin(api.uniqueEmail());
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"NOPE\",\"name\":\"x\",\"priceCents\":1,\"stock\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(delete("/api/products/1").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
    }

    @Test
    void aTamperedTokenIsRejected() throws Exception {
        String token = api.registerAndLogin(api.uniqueEmail());
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("aaa") ? "bbb" : "aaa");
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        Instant issued = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("late@shop.test").issuedAt(issued).expiresAt(issued.plus(Duration.ofHours(1)))
                .claim("uid", 1L).claim("roles", java.util.List.of("CUSTOMER")).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
        String email = api.uniqueEmail();
        api.registerAndLogin(email);
        String wrongPassword = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nobody@shop.test\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void theSigningSecretMustBeLongEnough() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new dev.canhdominich.shop.config.SecurityConfig().signingKey(new JwtProperties("too-short", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
