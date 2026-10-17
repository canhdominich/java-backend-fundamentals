package dev.canhdominich.spring.container.scanned;

import dev.canhdominich.spring.container.Greeter;
import org.springframework.stereotype.Component;

@Component("greeter")
public class ScannedGreeter extends Greeter {

    public ScannedGreeter() {
        super("hello from a scanned component");
    }
}
