package dev.canhdominich.spring.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DispatcherBehaviourTest {

    private static List<String> lines;

    @BeforeAll
    static void runOnce() throws Exception {
        lines = DispatcherDemo.run();
    }

    private static String line(String prefix) {
        return lines.stream().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void enableWebMvcRegistersTheStrategyBeansTheDispatcherUses() {
        assertThat(line("handler mappings:")).isEqualTo("handler mappings: [beanNameHandlerMapping, requestMappingHandlerMapping, routerFunctionMapping]");
        assertThat(line("handler adapters:")).contains("requestMappingHandlerAdapter");
        assertThat(line("view resolvers:")).isEqualTo("view resolvers: [mvcViewResolver]");
        assertThat(lines).contains("mapped: {GET [/hello]} -> hello", "mapped: {GET [/boom]} -> boom");
    }

    @Test
    void aRequestPassesFilterInterceptorControllerAndViewInOrder() {
        assertThat(line("GET /hello -> 200")).contains("<h1>Xin chao Spring MVC</h1>").endsWith(String.join("\n    ", "",
                "filter: before the servlet",
                "interceptor: preHandle for hello",
                "controller: hello()",
                "interceptor: postHandle, view = hello",
                "view: hello.jsp rendered",
                "interceptor: afterCompletion, exception = none",
                "filter: after the servlet"));
    }

    @Test
    void anUnmappedPathNeverReachesInterceptorsOrControllers() {
        assertThat(line("GET /nope -> 404")).isEqualTo("GET /nope -> 404\n    filter: before the servlet\n    filter: after the servlet");
    }

    @Test
    void aWrongMethodGets405WithTheAllowedMethods() {
        assertThat(line("POST /hello -> 405")).startsWith("POST /hello -> 405, Allow: GET");
    }

    @Test
    void anExceptionStillRunsAfterCompletionAndTheFilter() {
        assertThat(line("GET /boom -> 500")).endsWith("\n    interceptor: afterCompletion, exception = IllegalStateException\n    filter: after the servlet");
    }

    @Test
    void viewFilesUnderWebInfAreNotServedDirectly() {
        assertThat(line("GET /WEB-INF/views/hello.jsp")).isEqualTo("GET /WEB-INF/views/hello.jsp -> 404");
    }
}
