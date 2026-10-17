package dev.canhdominich.boot.ops;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoaderController {

    @GetMapping("/loader")
    public String loader() {
        return LoaderController.class.getClassLoader().getClass().getSimpleName();
    }
}
