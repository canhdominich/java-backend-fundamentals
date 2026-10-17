package dev.canhdominich.spring.container;

public class Engine {

    public Engine() {
        Events.record("constructing Engine");
    }
}
