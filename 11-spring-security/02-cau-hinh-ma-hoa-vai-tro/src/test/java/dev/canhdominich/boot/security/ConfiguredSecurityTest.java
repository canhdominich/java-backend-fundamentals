package dev.canhdominich.boot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfiguredSecurityTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = ConfiguredSecurityDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }


    @Test
    void rulesDecideWhoReachesWhatAndTheOrderOfStatusCodesIs401Then403() {
        assertThat(line("no credentials")).isEqualTo("no credentials         /public/info 200 | /api/me 401 | /admin/stats 401");
        assertThat(line("an (USER)")).isEqualTo("an (USER)              /public/info 200 | /api/me 200 | /admin/stats 403");
        assertThat(line("admin (ADMIN")).isEqualTo("admin (ADMIN, USER)    /public/info 200 | /api/me 200 | /admin/stats 200");
    }

    @Test
    void badCredentialsAreRejectedEvenOnAPermitAllPath() {
        assertThat(line("an, wrong password")).isEqualTo("an, wrong password     /public/info 401 | /api/me 401 | /admin/stats 401");
    }

    @Test
    void methodSecurityChecksTheOwner() {
        assertThat(lines).contains("an reads accounts/an -> 200", "an reads accounts/binh -> 403", "admin reads accounts/binh -> 200");
    }

    @Test
    void csrfIsIgnoredOnlyForTheApiPaths() {
        assertThat(lines).contains("POST /api/orders with Basic, no token -> 200");
    }

    @Test
    void passwordsAreStoredWithAPrefixAndASaltAndRejectedWithoutAPrefix() {
        assertThat(line("stored value starts with")).isEqualTo("stored value starts with: {bcrypt}$2a (length 68)");
        assertThat(lines).contains("same password encoded twice gives different values: true; both match: true", "wrong password matches: false", "explicit {noop} prefix still matches: true");
        assertThat(line("stored without a prefix")).contains("each password must have a password encoding prefix");
    }

    @Test
    void theCostOfBcryptGrowsWithTheStrengthParameter() {
        long four = Long.parseLong(line("strength 4:").replaceAll("[^0-9]+", ""));
        long ten = Long.parseLong(line("strength 10:").replaceAll("[^0-9]+", ""));
        long twelve = Long.parseLong(line("strength 12:").replaceAll("[^0-9]+", ""));
        long fourteen = Long.parseLong(line("strength 14:").replaceAll("[^0-9]+", ""));
        assertThat(four).isLessThan(ten);
        assertThat(ten).isLessThan(twelve);
        assertThat(twelve).isLessThan(fourteen);
        assertThat(fourteen).isGreaterThan(twelve * 2);
    }

    @Test
    void unknownUsersAreNotFasterToRejectThanKnownUsersWithAWrongPassword() {
        assertThat(line("unknown user:")).endsWith("similar: true");
    }

    @Test
    void aRuleAfterAnyRequestIsRejectedAtStartup() {
        assertThat(lines.get(lines.size() - 1)).isEqualTo("startup failed: IllegalStateException: Can't configure requestMatchers after anyRequest");
    }
}
