package dev.canhdominich.boot.aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Events {

    private static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());

    private Events() {
    }

    public static void record(String event) {
        LOG.add(event);
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
