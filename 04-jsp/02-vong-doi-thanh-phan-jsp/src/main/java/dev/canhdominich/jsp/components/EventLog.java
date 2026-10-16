package dev.canhdominich.jsp.components;

public final class EventLog {

    private EventLog() {
    }

    public static void record(String event) {
        System.out.println("  event: " + event);
    }
}
