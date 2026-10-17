package dev.canhdominich.spring.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @GetMapping("/hello")
    public String hello(Model model) {
        Trace.record("controller: hello()");
        model.addAttribute("message", "Xin chao Spring MVC");
        return "hello";
    }

    @GetMapping("/boom")
    public String boom() {
        Trace.record("controller: boom()");
        throw new IllegalStateException("something broke inside the controller");
    }
}
