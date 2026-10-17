package dev.canhdominich.spring.mvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Trace {

    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    private Trace() {
    }

    public static void record(String event) {
        EVENTS.add(event);
    }

    public static void clear() {
        EVENTS.clear();
    }

    public static List<String> all() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }
}
