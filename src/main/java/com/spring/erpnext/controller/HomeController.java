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

    @GetMapping("/page2")
    public String page2(@RequestParam(defaultValue = "1") int page, Model model) {
        // Données statiques (par exemple 15 éléments)
        List<String> elements = Arrays.asList(
                "Élément 1", "Élément 2", "Élément 3", "Élément 4", "Élément 5",
                "Élément 6", "Élément 7", "Élément 8", "Élément 9", "Élément 10",
                "Élément 11", "Élément 12", "Élément 13", "Élément 14", "Élément 15");

        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) elements.size() / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, elements.size());

        List<String> paginated = elements.subList(fromIndex, toIndex);

        model.addAttribute("page", "page2");
        model.addAttribute("elements", paginated);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "layout/base";
    }

    @GetMapping("/page3")
    public String page3(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String search,
            Model model) {

        // Données statiques : liste de tableaux [Nom, Email, Rôle]
        List<List<String>> utilisateurs = Arrays.asList(
                Arrays.asList("Alice", "alice@example.com", "Admin"),
                Arrays.asList("Bob", "bob@example.com", "User"),
                Arrays.asList("Charlie", "charlie@example.com", "Editor"),
                Arrays.asList("David", "david@example.com", "User"),
                Arrays.asList("Eve", "eve@example.com", "Admin"),
                Arrays.asList("Frank", "frank@example.com", "User"),
                Arrays.asList("Grace", "grace@example.com", "Editor"),
                Arrays.asList("Heidi", "heidi@example.com", "User"),
                Arrays.asList("Ivan", "ivan@example.com", "Admin"),
                Arrays.asList("Judy", "judy@example.com", "User"));

        // Filtrage
        if (search != null && !search.isEmpty()) {
            utilisateurs = utilisateurs.stream()
                    .filter(u -> u.stream().anyMatch(field -> field.toLowerCase().contains(search.toLowerCase())))
                    .toList();
        }

        // Pagination
        int pageSize = 4;
        int totalPages = (int) Math.ceil((double) utilisateurs.size() / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, utilisateurs.size());
        List<List<String>> paginated = utilisateurs.subList(fromIndex, toIndex);

        model.addAttribute("page", "page3");
        model.addAttribute("utilisateurs", paginated);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);

        return "layout/base";
    }

}
