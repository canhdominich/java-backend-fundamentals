package dev.canhdominich.cloud.trace;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpanCollector extends SpanHandler {

    public static final List<MutableSpan> FINISHED = new CopyOnWriteArrayList<>();

    @Override
    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
        FINISHED.add(span);
        return true;
    }
}
