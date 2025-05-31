package com.spring.erpnext.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImportController {

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("page", "import"); // indique quel fragment charger dans layout.html
        return "layout/base"; // on retourne la page principale (layout)
    }

    @PostMapping("/import")
    public String handleImport(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam("file3") MultipartFile file3,
            Model model) {
        try {
            if (!file1.isEmpty()) {
                System.out.println("Fichier 1: " + file1.getOriginalFilename());
            }
            if (!file2.isEmpty()) {
                System.out.println("Fichier 2: " + file2.getOriginalFilename());
            }
            if (!file3.isEmpty()) {
                System.out.println("Fichier 3: " + file3.getOriginalFilename());
            }

            model.addAttribute("message", "Importation réussie !");
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l'importation : " + e.getMessage());
        }

        return "import";
    }
}
