package com.spring.erpnext.controller;

import com.spring.erpnext.model.Company;
import com.spring.erpnext.model.Employee;
import com.spring.erpnext.service.BaseSalaryService;
import com.spring.erpnext.service.CompanyService;
import com.spring.erpnext.service.EmployeeService;
import com.spring.erpnext.service.TestService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class EmployeeController {

    private final EmployeeService employeeService;
    private final CompanyService companyService;
    private final TestService testService;
    private final BaseSalaryService baseSalaryService;

    @Autowired
    public EmployeeController(EmployeeService employeeService, CompanyService companyService, TestService testService,
            BaseSalaryService baseSalaryService) {
        this.employeeService = employeeService;
        this.companyService = companyService;
        this.testService = testService;
        this.baseSalaryService = baseSalaryService;
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

        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            employees = employees.stream()
                    .filter(e -> (e.getLast_name() != null && e.getLast_name().toLowerCase().contains(lowerSearch)) ||
                            (e.getFirst_name() != null && e.getFirst_name().toLowerCase().contains(lowerSearch)) ||
                            (e.getMiddle_name() != null && e.getMiddle_name().toLowerCase().contains(lowerSearch)))
                    .toList();
        }

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

    @GetMapping("/employees-add")
    public String InsertEmployePage(HttpSession session, Model model) {
        if (session.getAttribute("sid") == null) {
            return "redirect:/login";
        }

        List<Company> company = companyService.getAllCompanies(session);

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("company", company);
        model.addAttribute("page", "employees-add");

        model.addAttribute("employee", new Employee());

        return "layout/base";
    }

    @PostMapping("/employees-add")
    public String submitEmployeeForm(
            @RequestParam("name") String name,
            @RequestParam("first_name") String firstName,
            @RequestParam(value = "middle_name", required = false) String middleName,
            @RequestParam("last_name") String lastName,
            @RequestParam("date_of_birth") String dateOfBirth,
            @RequestParam("date_of_joining") String dateOfJoining,
            @RequestParam("gender") String gender,
            @RequestParam("company") String company,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Employee employee = new Employee();
        employee.setName(name);
        employee.setFirst_name(firstName);
        employee.setMiddle_name(middleName);
        employee.setLast_name(lastName);
        employee.setDate_of_birth(dateOfBirth);
        employee.setDate_of_joining(dateOfJoining);
        employee.setGender(gender);
        employee.setCompany(company);

        boolean success = employeeService.insertEmployee(employee, session);

        if (success) {
            redirectAttributes.addFlashAttribute("message", "✅ Employé ajouté avec succès.");
        } else {
            redirectAttributes.addFlashAttribute("error", "❌ Échec de l'ajout de l'employé.");
        }

        return "redirect:/employees-add";
    }

    @GetMapping("/employex")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = testService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employee/info/{employeeId}")
    @ResponseBody
    public Map<String, String> getSalaryStructureAndCompany(@PathVariable String employeeId, HttpSession session) {
        Map<String, String> infos = baseSalaryService.getSalaryStructureAndCompany(session, employeeId);

        if (infos != null) {
            return infos;
        } else {
            // Retourne une map vide ou un message d'erreur, selon ta préférence
            return Collections.emptyMap();
        }
    }

    @GetMapping("/employeeBD")
    public String getEmployeesPage(Model model) {
        List<Employee> employees = testService.getAllEmployees(); // récupère les données
        model.addAttribute("employees", employees); // ajoute au modèle
        model.addAttribute("page", "employeeBD"); // autre attribut si besoin
        return "layout/base";
    }

}
