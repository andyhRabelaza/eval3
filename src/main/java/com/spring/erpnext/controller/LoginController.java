package com.spring.erpnext.controller;

import com.spring.erpnext.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Map<String, String> sessionData = authService.loginToErpNext(username, password);

        if (sessionData != null) {
            session.setAttribute("sid", sessionData.get("sid"));
            session.setAttribute("username", sessionData.get("username"));
            session.setAttribute("password", sessionData.get("password"));
            return "redirect:/index";
        } else {
            model.addAttribute("error", "Connexion échouée !");
            return "login";
        }
    }

    
}
