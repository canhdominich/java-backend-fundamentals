package dev.canhdominich.spring.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Events {

    private static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());

    private Events() {
    }

    public static void record(String event) {
        LOG.add(event);
        System.out.println("  " + LOG.size() + ". " + event);
    }

    public static void clear() {
        LOG.clear();
    }

    public static List<String> all() {
        synchronized (LOG) {
            return new ArrayList<>(LOG);
        }
    }
}
