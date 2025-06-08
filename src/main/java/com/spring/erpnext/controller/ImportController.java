package com.spring.erpnext.controller;

import com.spring.erpnext.service.ImportService;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

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

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "import");

        if (!file1.isEmpty()) {
            try {
                System.out.println("Fichier 1: " + file1.getOriginalFilename());
                importService.readFileLines(file1);
                importService.processFile1(file1, session);
                successMessages.add("✅ Fichier 1 importé avec succès.");
            } catch (Exception e) {
                errorMessages.add("❌ Erreur lors de l'import du fichier 1 : " + e.getMessage());
            }
        }

        if (!file2.isEmpty()) {
            try {
                System.out.println("Fichier 2: " + file2.getOriginalFilename());
                importService.readFileLines(file2);
                importService.processFile2(file2, session);
                successMessages.add("✅ Fichier 2 importé avec succès.");
            } catch (Exception e) {
                errorMessages.add("❌ Erreur lors de l'import du fichier 2 : " + e.getMessage());
            }
        }

        if (!file3.isEmpty()) {
            try {
                System.out.println("Fichier 3: " + file3.getOriginalFilename());
                importService.readFileLines(file3);
                importService.processFile3(file3, session);
                successMessages.add("✅ Fichier 3 importé avec succès.");
            } catch (Exception e) {
                errorMessages.add("❌ Erreur lors de l'import du fichier 3 : " + e.getMessage());
            }
        }

        model.addAttribute("successMessages", successMessages);
        model.addAttribute("errorMessages", errorMessages);

        return "layout/base";
    }

}
