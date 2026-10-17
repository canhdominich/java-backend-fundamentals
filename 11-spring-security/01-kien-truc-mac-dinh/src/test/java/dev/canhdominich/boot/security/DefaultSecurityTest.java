package dev.canhdominich.boot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DefaultSecurityTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = DefaultSecurityDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void theDefaultChainHasSixteenFiltersEndingWithAuthorization() {
        String filters = line("filters in the default chain");
        assertThat(filters).contains("[DisableEncodeUrlFilter, WebAsyncManagerIntegrationFilter, SecurityContextHolderFilter, HeaderWriterFilter, CsrfFilter, LogoutFilter, UsernamePasswordAuthenticationFilter");
        assertThat(filters).endsWith("ExceptionTranslationFilter, AuthorizationFilter]");
        assertThat(filters.split(",")).hasSize(16);
    }

    @Test
    void everythingRequiresAuthenticationAndTheAnswerDependsOnTheClient() {
        assertThat(line("GET /hello (no credentials) ->")).isEqualTo("GET /hello (no credentials) -> 401, WWW-Authenticate: Basic realm=\"Realm\", charset=\"UTF-8\"");
        assertThat(line("GET /hello (no credentials) Accept text/html")).isEqualTo("GET /hello (no credentials) Accept text/html -> 302, Location: /login");
        assertThat(line("GET /hello (wrong password)")).contains("-> 401");
        assertThat(line("GET /hello (right password)")).endsWith("-> 200, body {\"message\":\"hello user\"}");
    }

    @Test
    void securityRunsBeforeRoutingSoAnUnknownPathIs401ForStrangersAnd404ForUsers() {
        assertThat(line("GET /no-such-page (no credentials)")).contains("-> 401");
        assertThat(line("GET /no-such-page (right password)")).endsWith("-> 404");
    }

    @Test
    void secureHeadersAreAddedByDefault() {
        assertThat(line("response headers")).isEqualTo("response headers: [cache-control, content-type, expires, pragma, x-content-type-options, x-frame-options, x-xss-protection]");
    }

    @Test
    void csrfProtectionRunsBeforeAuthenticationAndFormLoginWorksWithASession() {
        assertThat(line("POST /orders (right password)")).contains("-> 401");
        assertThat(line("GET /login")).contains("-> 200, form has username, password and _csrf fields: true, session cookie attributes: [Path=/, HttpOnly]");
        assertThat(line("POST /login with a wrong password")).endsWith("-> 302, Location /login?error");
        assertThat(line("POST /login with the right password")).endsWith("-> 302, Location /, session id changed: true");
        assertThat(line("GET /hello with only the session cookie")).endsWith("-> 200 {\"message\":\"hello user\"}");
        assertThat(line("POST /orders with the session cookie")).endsWith("-> 403");
    }
}
