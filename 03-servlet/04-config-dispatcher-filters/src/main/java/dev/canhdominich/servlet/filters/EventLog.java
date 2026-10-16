package dev.canhdominich.servlet.filters;

public final class EventLog {

    private EventLog() {
    }

    public static void record(String event) {
        System.out.println("  event: " + event);
    }
}
