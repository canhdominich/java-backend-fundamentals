package dev.canhdominich.spring.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/pages")
public class PageController {

    @GetMapping("/students/new")
    @ResponseBody
    public String form() {
        return "form for a new student";
    }

    @GetMapping("/students/{id}")
    @ResponseBody
    public String student(@PathVariable int id) {
        return "student " + id;
    }

    @GetMapping("/products/{sku}/reviews/{id}")
    @ResponseBody
    public String review(@PathVariable String sku, @PathVariable int id) {
        return "review " + id + " of " + sku;
    }

    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam String q, @RequestParam(defaultValue = "1") int page) {
        return "q=" + q + " page=" + page;
    }

    @PostMapping("/register")
    @ResponseBody
    public String register(@ModelAttribute Registration registration) {
        return "registered " + registration.getName() + " aged " + registration.getAge();
    }

    @GetMapping("/trace")
    @ResponseBody
    public String trace(@RequestHeader(value = "X-Trace", defaultValue = "none") String trace) {
        return "trace=" + trace;
    }

    @GetMapping("/view")
    public String view(Model model) {
        model.addAttribute("title", "Danh sach sinh vien");
        return "students";
    }
}
