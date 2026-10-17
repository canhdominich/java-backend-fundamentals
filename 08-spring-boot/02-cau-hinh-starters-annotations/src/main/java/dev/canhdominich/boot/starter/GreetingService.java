package dev.canhdominich.boot.starter;

public class GreetingService {

    private final String message;

    public GreetingService(String message) {
        this.message = message;
    }

    public String greet(String name) {
        return message + ", " + name;
    }
}
