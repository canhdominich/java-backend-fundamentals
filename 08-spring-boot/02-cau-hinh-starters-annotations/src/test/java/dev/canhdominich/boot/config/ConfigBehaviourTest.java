package dev.canhdominich.boot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = ConfigDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void propertiesBindToATypedObjectWithConversionsAndRelaxedNames() {
        assertThat(lines).contains("AppProperties[name=Course Platform, maxSize=10, timeout=PT30S, admins=[an, binh], mail=Mail[host=localhost, port=25]]");
        assertThat(lines).contains("--app.max-size=9 -> maxSize 9", "--app.maxSize=7 -> maxSize 7", "--app.max_size=8 -> maxSize 8");
    }

    @Test
    void commandLineBeatsEnvironmentBeatsProfileFileBeatsBaseFile() {
        assertThat(lines).contains(
                "application.properties only -> Course Platform",
                "profile dev -> Course Platform (dev)",
                "profile dev + system property -Dapp.name=sys -> sys",
                "profile dev + environment APP_NAME=env -> env",
                "profile dev + environment APP_NAME=env + --app.name=cli -> cli");
    }

    @Test
    void profilesSwitchBeansAndOverrideOnlyTheKeysTheyDefine() {
        assertThat(lines).contains("no profile: DevOnly present = false", "profile dev: DevOnly present = true", "profile dev: maxSize = 50");
    }

    @Test
    void badConfigurationStopsStartupWithAClearMessage() {
        assertThat(line("--app.max-size=0")).contains("ConfigurationPropertiesBindException").contains("BindValidationException");
        assertThat(line("--app.timeout=abc")).endsWith("IllegalArgumentException: 'abc' is not a valid duration");
        assertThat(line("@Value")).contains("Could not resolve placeholder 'app.missing'");
    }

    @Test
    void anAutoConfigurationAppliesByDefaultCanBeSwitchedOffAndYieldsToYourOwnBean() {
        assertThat(lines).contains(
                "default -> hello, An",
                "--greeting.message=hi -> hi, An",
                "--greeting.enabled=false -> GreetingService beans: 0",
                "own GreetingService bean in OverrideConfig -> custom, An",
                "condition report for GreetingAutoConfiguration: matched");
    }

    @Test
    void componentScanningStopsAtThePackageOfTheApplicationClass() {
        assertThat(line("OutsideComponent beans")).endsWith("= 0");
    }
}
