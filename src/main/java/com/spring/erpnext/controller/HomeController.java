package com.spring.erpnext.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/index")
    public String home(Model model) {
        model.addAttribute("page", "index");
        return "layout/base";
    }

}
