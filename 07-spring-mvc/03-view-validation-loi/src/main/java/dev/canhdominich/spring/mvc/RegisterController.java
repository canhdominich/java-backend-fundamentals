package dev.canhdominich.spring.mvc;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final UserStore store;

    public RegisterController(UserStore store) {
        this.store = store;
    }

    @GetMapping("/register")
    public String form(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String submit(@Valid @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "register";
        }
        store.add(user);
        redirect.addFlashAttribute("message", "Saved " + user.getName());
        return "redirect:/registered";
    }

    @GetMapping("/registered")
    public String done(Model model) {
        model.addAttribute("count", store.size());
        return "registered";
    }

    @PostMapping("/register-no-redirect")
    public String noRedirect(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        store.add(user);
        model.addAttribute("message", "Saved " + user.getName());
        model.addAttribute("count", store.size());
        return "registered";
    }

    @PostMapping("/register-strict")
    @ResponseBody
    public String strict(@Valid @ModelAttribute User user) {
        return "accepted " + user.getName();
    }
}
