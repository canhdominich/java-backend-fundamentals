package dev.canhdominich.junit.orders;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.ArrayList;
import java.util.List;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

public final class ProbeRunner {

    private ProbeRunner() {
    }

    public record Outcome(String test, String status, String cause) {
    }

    public static List<Outcome> run(Class<?> probe, String... includeTags) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(probe));
        if (includeTags.length > 0) {
            builder.filters(TagFilter.includeTags(includeTags));
        }
        LauncherDiscoveryRequest request = builder.build();
        List<Outcome> outcomes = new ArrayList<>();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, new TestExecutionListener() {
            @Override
            public void executionSkipped(TestIdentifier identifier, String reason) {
                if (identifier.isTest()) {
                    outcomes.add(new Outcome(identifier.getDisplayName(), "SKIPPED", reason));
                }
            }

            @Override
            public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
                if (identifier.isTest()) {
                    String cause = result.getThrowable().map(t -> t.getClass().getSimpleName()).orElse("");
                    outcomes.add(new Outcome(identifier.getDisplayName(), result.getStatus().name(), cause));
                }
            }
        });
        return outcomes;
    }
}
