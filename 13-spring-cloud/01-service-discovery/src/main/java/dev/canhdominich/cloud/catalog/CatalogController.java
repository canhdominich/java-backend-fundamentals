package dev.canhdominich.cloud.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {

    private final String instance;

    public CatalogController(@Value("${eureka.instance.instance-id}") String instance) {
        this.instance = instance;
    }

    @GetMapping("/whoami")
    public String whoami() {
        return instance;
    }
}
