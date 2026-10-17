package dev.canhdominich.cloud.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

public final class TraceInfo {

    private TraceInfo() {
    }

    public static String describe(String service, Tracer tracer, HttpServletRequest request) {
        Span span = tracer.currentSpan();
        if (span == null) {
            return "service=" + service + " traceId=none incoming=" + request.getHeader("traceparent");
        }
        String parent = span.context().parentId();
        return "service=" + service
                + " traceId=" + span.context().traceId()
                + " spanId=" + span.context().spanId()
                + " parentId=" + (parent == null || parent.isEmpty() ? "none" : parent)
                + " mdcTraceId=" + MDC.get("traceId")
                + " incoming=" + request.getHeader("traceparent");
    }
}
