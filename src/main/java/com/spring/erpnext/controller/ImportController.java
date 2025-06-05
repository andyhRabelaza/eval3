package com.spring.erpnext.controller;

import com.spring.erpnext.service.ImportService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/import")
    public String importPage(Model model, HttpSession session) {

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "import");
        return "layout/base";
    }

    @PostMapping("/import")
    public String handleImport(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam("file3") MultipartFile file3,
            HttpSession session,
            Model model) {
        try {
            if (!file1.isEmpty()) {
                System.out.println("Fichier 1: " + file1.getOriginalFilename());
                importService.readFileLines(file1);
                importService.processFile1(file1, session);
            }
            if (!file2.isEmpty()) {
                System.out.println("Fichier 2: " + file2.getOriginalFilename());
                importService.readFileLines(file2);
                importService.processFile2(file2, session);
            }
            if (!file3.isEmpty()) {
                System.out.println("Fichier 3: " + file3.getOriginalFilename());
                importService.readFileLines(file3);
            }

            model.addAttribute("message", "Importation réussie !");
            model.addAttribute("page", "import");
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l'importation : " + e.getMessage());
        }

        return "layout/base";
    }
}
