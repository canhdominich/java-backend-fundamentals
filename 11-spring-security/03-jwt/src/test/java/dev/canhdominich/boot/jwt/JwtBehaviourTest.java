package dev.canhdominich.boot.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JwtBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = JwtDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void loginReturnsATokenOnlyForRightCredentials() {
        assertThat(lines).contains("login an -> 200, token received: true", "login admin -> 200, token received: true", "login with a wrong password -> 401");
    }

    @Test
    void aTokenIsThreeReadablePartsAndOnlyCarriesWhatWeChose() {
        assertThat(lines).contains("parts separated by dots: 3", "header: {\"alg\":\"HS256\"}", "payload claims: [exp, iat, roles, sub], sub an, roles [USER], lifetime 900 s");
    }

    @Test
    void theApiAcceptsBearerTokensAndAppliesRoleRulesWithoutASession() {
        assertThat(line("no token")).startsWith("no token -> 401, WWW-Authenticate: Bearer");
        assertThat(line("an token on /api/me")).isEqualTo("an token on /api/me -> 200, {\"authorities\":[\"FACTOR_BEARER\",\"ROLE_USER\"],\"user\":\"an\"}");
        assertThat(line("an token on /admin/stats")).contains("-> 403").contains("insufficient_scope");
        assertThat(line("admin token on /admin/stats")).isEqualTo("admin token on /admin/stats -> 200, {\"stats\":\"secret numbers\"}");
        assertThat(lines).contains("session cookie set by the server: none");
    }

    @Test
    void forgedAndForeignAndMalformedTokensAreRefused() {
        assertThat(line("payload changed to ADMIN")).contains("-> 401").contains("Signed JWT rejected: Invalid signature");
        assertThat(line("alg none")).contains("-> 401").contains("Unsupported algorithm of none");
        assertThat(line("signed with another secret")).contains("-> 401").contains("Invalid signature");
        assertThat(line("garbage")).contains("-> 401").contains("Malformed token");
    }

    @Test
    void expiryHasAOneMinuteLeewayAndNothingRevokesAToken() {
        assertThat(line("expired 2 hours ago")).contains("-> 401").contains("Jwt expired at");
        assertThat(line("expired 30 seconds ago")).contains("-> 200");
        assertThat(line("expired 90 seconds ago")).contains("-> 401");
        assertThat(lines).contains("login after the user was deleted -> 401");
        assertThat(line("old token of the deleted user")).contains("-> 200");
    }
}
