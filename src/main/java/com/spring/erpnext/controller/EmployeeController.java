package com.spring.erpnext.controller;

import com.spring.erpnext.model.Employee;
import com.spring.erpnext.service.EmployeeService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public String showEmployeesPage(
            HttpSession session,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            Model model) {

        List<Employee> employees = employeeService.getAllEmployees(session);

        if (employees == null) {
            return "redirect:/login";
        }

        // Recherche
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            employees = employees.stream()
                    .filter(e -> (e.getLast_name() != null && e.getLast_name().toLowerCase().contains(lowerSearch)) ||
                            (e.getFirst_name() != null && e.getFirst_name().toLowerCase().contains(lowerSearch)) ||
                            (e.getMiddle_name() != null && e.getMiddle_name().toLowerCase().contains(lowerSearch)))
                    .toList();
        }

        // Pagination
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) employees.size() / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, employees.size());

        List<Employee> paginated = employees.subList(fromIndex, toIndex);
        String username = (String) session.getAttribute("username");

        model.addAttribute("username", username);
        model.addAttribute("employees", paginated);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);
        model.addAttribute("page", "employees");

        return "layout/base";
    }

    @GetMapping("/employees/delete/{name}")
    public String deleteEmployee(
            @PathVariable("name") String name,
            HttpSession session) {

        employeeService.deleteEmployee(name, session);

        return "redirect:/employees";
    }

}
