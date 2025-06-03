package com.spring.erpnext.controller;

import com.spring.erpnext.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private final EmployeeService employeeService;

    @Autowired
    public HomeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/index")
    public String home(HttpSession session, Model model) {
        if (session.getAttribute("sid") == null) {
            return "redirect:/login";
        }

        int employeeCount = employeeService.getEmployeeCount(session);
        model.addAttribute("employeeCount", employeeCount);
        model.addAttribute("page", "index");

        return "layout/base";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Invalide la session existante
        return "redirect:/login"; // Redirige vers la page de connexion
    }
}
