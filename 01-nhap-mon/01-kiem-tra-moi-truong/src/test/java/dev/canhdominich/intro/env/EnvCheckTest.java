package dev.canhdominich.intro.env;

import static org.assertj.core.api.Assertions.assertThat;

import dev.canhdominich.intro.env.EnvCheck.Check;
import dev.canhdominich.intro.env.EnvCheck.Level;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnvCheckTest {

    @Test
    void thisMachineMeetsTheMinimumForTheCourse() {
        List<Check> checks = EnvCheck.run();

        assertThat(checks).noneMatch(check -> check.level() == Level.FAIL);
        assertThat(checks).hasSize(6);
    }

    @Test
    void aMissingCommandIsReportedAsNull() {
        assertThat(EnvCheck.firstLine("this-command-does-not-exist")).isNull();
    }

    @Test
    void aClosedPortIsNotReachable() {
        assertThat(EnvCheck.reachable("localhost", 1)).isFalse();
    }
}
