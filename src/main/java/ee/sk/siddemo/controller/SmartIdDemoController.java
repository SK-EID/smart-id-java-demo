package ee.sk.siddemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SmartIdDemoController {

    @GetMapping("/")
    public String introductionPage(Model model) {
        model.addAttribute("activeTab", null);
        return "index";
    }
}
