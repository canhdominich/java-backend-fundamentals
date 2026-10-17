package dev.canhdominich.spring.container;

public class Car {

    private final Engine engine;

    public Car(Engine engine) {
        this.engine = engine;
        Events.record("constructing Car");
    }

    public Engine engine() {
        return engine;
    }
}
