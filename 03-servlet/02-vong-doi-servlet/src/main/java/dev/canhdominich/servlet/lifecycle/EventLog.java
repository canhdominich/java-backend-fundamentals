package dev.canhdominich.servlet.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EventLog {

    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    private EventLog() {
    }

    public static void record(String event) {
        EVENTS.add(event);
        System.out.println("  event: " + event);
    }

    public static List<String> snapshot() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }

    public static void clear() {
        EVENTS.clear();
    }
}
