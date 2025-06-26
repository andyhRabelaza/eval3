package com.spring.erpnext.controller;

import com.spring.erpnext.model.BaseSalary;
import com.spring.erpnext.model.Company;
import com.spring.erpnext.model.Component;
import com.spring.erpnext.model.Deduction;
import com.spring.erpnext.model.Earning;
import com.spring.erpnext.model.Employee;
import com.spring.erpnext.model.SalarySlip;
import com.spring.erpnext.service.BaseSalaryService;
import com.spring.erpnext.service.CompanyService;
import com.spring.erpnext.service.ComponentService;
import com.spring.erpnext.service.EmployeeService;
import com.spring.erpnext.service.SalaryService;

import jakarta.servlet.http.HttpServletResponse;
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
// import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.time.YearMonth;

@Controller
public class SalaryController {

    private final SalaryService salaryService;
    private final BaseSalaryService baseSalaryService;
    private final EmployeeService employeeService;
    private final CompanyService companyService;
    private final ComponentService componentService;

    @Autowired
    public SalaryController(SalaryService salaryService, BaseSalaryService baseSalaryService,
            EmployeeService employeeService, CompanyService companyService, ComponentService componentService) {
        this.salaryService = salaryService;
        this.baseSalaryService = baseSalaryService;
        this.employeeService = employeeService;
        this.companyService = companyService;
        this.componentService = componentService;
    }

    @GetMapping("/salary")
    public String home(
            Model model,
            HttpSession session,
            @RequestParam(defaultValue = "1") int page) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        List<SalarySlip> salarySlips = salaryService.getAllSalaries(session);

        // Pagination
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) salarySlips.size() / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, salarySlips.size());

        List<SalarySlip> paginatedSalaries = salarySlips.subList(fromIndex, toIndex);

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("salarySlips", paginatedSalaries);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("page", "salary");

        return "layout/base";
    }

    @GetMapping("/salary-employe")
    public String filtreEmployeEtSalaire(
            @RequestParam(value = "monthYear", required = false) String monthYear,
            @RequestParam(defaultValue = "1") int page,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        List<SalarySlip> salarySlips;
        Integer year = null;
        Integer month = null;

        if (monthYear != null && !monthYear.isEmpty()) {
            try {
                YearMonth parsed = YearMonth.parse(monthYear);
                year = parsed.getYear();
                month = parsed.getMonthValue();

                model.addAttribute("selectedMonth", monthYear);
                salarySlips = salaryService.getSalariesByMonth(session, year, month);
            } catch (DateTimeException e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Format de date invalide. Veuillez utiliser AAAA-MM.");
                return "redirect:/salary-employe";
            }
        } else {
            salarySlips = salaryService.getAllSalaries(session);
        }

        // Pagination
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) salarySlips.size() / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, salarySlips.size());
        List<SalarySlip> paginatedSlips = salarySlips.subList(fromIndex, toIndex);

        double totalGrossPay = salarySlips.stream()
                .filter(slip -> slip.getGross_pay() != null)
                .mapToDouble(SalarySlip::getGross_pay)
                .sum();

        double totalNetPay = salarySlips.stream()
                .filter(slip -> slip.getNet_pay() != null)
                .mapToDouble(SalarySlip::getNet_pay)
                .sum();

        String username = (String) session.getAttribute("username");

        model.addAttribute("username", username);
        model.addAttribute("salarySlips", paginatedSlips);
        model.addAttribute("totalGrossPay", totalGrossPay);
        model.addAttribute("totalNetPay", totalNetPay);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("selectedMonth", monthYear);
        model.addAttribute("page", "salary-employe");

        return "layout/base";
    }

    @GetMapping("/salary/export/pdf")
    public void exportSalaryPdf(String name, HttpSession session, HttpServletResponse response)
            throws IOException, DocumentException {

        SalarySlip salarySlip = salaryService.getSalaryByName(name, session);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=salary_" + name + ".pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        BaseColor darkBlue = new BaseColor(30, 60, 110);
        BaseColor lightGray = new BaseColor(240, 240, 240);
        BaseColor green = new BaseColor(0, 153, 76);
        BaseColor red = new BaseColor(204, 0, 0);
        BaseColor white = BaseColor.WHITE;

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, darkBlue);
        Font sectionHeaderFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, darkBlue);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 9);

        PdfContentByte cb = writer.getDirectContent();
        ColumnText ct = new ColumnText(cb);
        float margin = 36f;
        Rectangle rect = document.getPageSize();
        ct.setSimpleColumn(margin, margin, rect.getWidth() - margin, rect.getHeight() - margin);

        Paragraph title = new Paragraph("Bulletin de salaire", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        ct.addElement(title);

        // Section: Informations générales
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new int[] { 3, 7 });
        infoTable.setSpacingAfter(15);
        PdfPCell infoHeader = new PdfPCell(new Phrase("Informations générales", sectionHeaderFont));
        infoHeader.setColspan(2);
        infoHeader.setBackgroundColor(darkBlue);
        infoHeader.setPadding(6);
        infoHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoTable.addCell(infoHeader);

        BiFunction<String, Font, PdfPCell> labelCell = (text, font) -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(5);
            return cell;
        };

        BiFunction<String, Font, PdfPCell> valueCell = (text, font) -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(5);
            return cell;
        };

        infoTable.addCell(labelCell.apply("Nom du bulletin", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getName(), valueFont));
        infoTable.addCell(labelCell.apply("Employé", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getEmployee_name(), valueFont));
        infoTable.addCell(labelCell.apply("Période", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getStart_date() + " au " + salarySlip.getEnd_date(), valueFont));
        infoTable.addCell(labelCell.apply("Date de publication", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getPosting_date(), valueFont));
        infoTable.addCell(labelCell.apply("Structure salariale", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getSalary_structure(), valueFont));
        infoTable.addCell(labelCell.apply("Société", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getCompany(), valueFont));
        infoTable.addCell(labelCell.apply("Statut", labelFont));
        infoTable.addCell(valueCell.apply(salarySlip.getStatus(), valueFont));
        ct.addElement(infoTable);

        // Section: Détails de la paie
        PdfPTable payDetails = new PdfPTable(2);
        payDetails.setWidthPercentage(100);
        payDetails.setWidths(new int[] { 6, 4 });
        payDetails.setSpacingAfter(15);
        PdfPCell payHeader = new PdfPCell(new Phrase("Détails de la paie", sectionHeaderFont));
        payHeader.setColspan(2);
        payHeader.setBackgroundColor(green);
        payHeader.setPadding(6);
        payHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        payDetails.addCell(payHeader);

        Function<String, PdfPCell> grayLabel = text -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, labelFont));
            cell.setBackgroundColor(lightGray);
            cell.setPadding(5);
            return cell;
        };

        Function<Double, PdfPCell> dynamicValue = val -> {
            BaseColor color = val == 0 ? red : BaseColor.BLACK;
            Font f = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, color);
            PdfPCell cell = new PdfPCell(new Phrase(String.format("%.2f", val), f));
            cell.setPadding(5);
            return cell;
        };

        payDetails.addCell(grayLabel.apply("Salaire brut (" + salarySlip.getCurrency() + ")"));
        payDetails.addCell(dynamicValue.apply(salarySlip.getGross_pay()));
        payDetails.addCell(grayLabel.apply("Salaire net (" + salarySlip.getCurrency() + ")"));
        payDetails.addCell(dynamicValue.apply(salarySlip.getNet_pay()));
        payDetails.addCell(grayLabel.apply("Jours payés"));
        payDetails.addCell(dynamicValue.apply((double) salarySlip.getPayment_days()));
        payDetails.addCell(grayLabel.apply("Congés sans solde"));
        payDetails.addCell(dynamicValue.apply((double) salarySlip.getLeave_without_pay()));
        ct.addElement(payDetails);

        // Section: Earnings
        PdfPTable earnings = new PdfPTable(2);
        earnings.setWidthPercentage(100);
        earnings.setWidths(new int[] { 6, 4 });
        earnings.setSpacingAfter(15);
        PdfPCell earnHeader = new PdfPCell(new Phrase("Earnings", sectionHeaderFont));
        earnHeader.setColspan(2);
        earnHeader.setBackgroundColor(darkBlue);
        earnHeader.setPadding(6);
        earnHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        earnings.addCell(earnHeader);

        earnings.addCell(grayLabel.apply("Composant"));
        earnings.addCell(grayLabel.apply("Montant (" + salarySlip.getCurrency() + ")"));
        for (Earning e : salarySlip.getEarnings()) {
            earnings.addCell(valueCell.apply(e.getSalary_component(), valueFont));
            earnings.addCell(dynamicValue.apply(e.getAmount()));
        }
        ct.addElement(earnings);

        // Section: Deductions
        PdfPTable deductions = new PdfPTable(2);
        deductions.setWidthPercentage(100);
        deductions.setWidths(new int[] { 6, 4 });
        deductions.setSpacingAfter(15);
        PdfPCell deducHeader = new PdfPCell(new Phrase("Deductions", sectionHeaderFont));
        deducHeader.setColspan(2);
        deducHeader.setBackgroundColor(new BaseColor(51, 122, 183));
        deducHeader.setPadding(6);
        deducHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        deductions.addCell(deducHeader);

        deductions.addCell(grayLabel.apply("Composant"));
        deductions.addCell(grayLabel.apply("Montant (" + salarySlip.getCurrency() + ")"));
        for (Deduction d : salarySlip.getDeductions()) {
            deductions.addCell(valueCell.apply(d.getSalary_component(), valueFont));
            deductions.addCell(dynamicValue.apply(d.getAmount()));
        }
        ct.addElement(deductions);

        Paragraph footer = new Paragraph("\nMerci pour votre travail au sein de " + salarySlip.getCompany(), valueFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        ct.addElement(footer);

        ct.go();
        document.close();
    }

    @GetMapping("/salary-statistiques")
    public String getSalariesSummary(@RequestParam(required = false) Integer year,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        List<SalarySlip> salaries;

        if (year == null) {
            year = 2025;
        }

        if (year != null) {
            salaries = salaryService.getSalariesByYear(session, year);
            model.addAttribute("selectedYear", year);
        } else {
            salaries = salaryService.getAllSalaries(session);
        }

        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

        // 1) Regrouper les salaires par YearMonth
        Map<YearMonth, List<SalarySlip>> groupedByYM = new TreeMap<>();

        for (SalarySlip slip : salaries) {
            String rawMonth = slip.getStartDateMonth();
            if (rawMonth == null || rawMonth.isBlank()) {
                continue;
            }
            if (year == null) {
                System.err.println("Aucune année précisée pour le parsing de: " + rawMonth);
                continue;
            }

            String fullDate = rawMonth.trim() + " " + year;
            try {
                YearMonth ym = YearMonth.parse(fullDate, ymFormatter);
                groupedByYM.computeIfAbsent(ym, k -> new ArrayList<>()).add(slip);
            } catch (DateTimeParseException e) {
                System.err.println("Erreur de parsing pour: '" + fullDate + "'");
                e.printStackTrace();
            }
        }

        // 2) Initialiser monthlyTotals avec TOUS les mois de l'année, valeurs à zéro
        LinkedHashMap<String, Map<String, Double>> monthlyTotals = new LinkedHashMap<>();

        YearMonth startMonth = YearMonth.of(year, 1);
        for (int m = 0; m < 12; m++) {
            YearMonth current = startMonth.plusMonths(m);
            String monthLabel = current.format(displayFormatter);
            monthLabel = monthLabel.substring(0, 1).toUpperCase() + monthLabel.substring(1);

            Map<String, Double> zeroTotals = new HashMap<>();
            zeroTotals.put("net", 0.0);
            zeroTotals.put("brut", 0.0);
            zeroTotals.put("deduction", 0.0);

            monthlyTotals.put(monthLabel, zeroTotals);
        }

        // 3) Remplir monthlyTotals avec les vraies données si elles existent
        for (Map.Entry<YearMonth, List<SalarySlip>> entry : groupedByYM.entrySet()) {
            YearMonth ym = entry.getKey();
            List<SalarySlip> slips = entry.getValue();

            double totalNet = slips.stream().mapToDouble(s -> s.getNet_pay() != null ? s.getNet_pay() : 0).sum();
            double totalBrut = slips.stream().mapToDouble(s -> s.getGross_pay() != null ? s.getGross_pay() : 0).sum();
            double totalDeduction = slips.stream()
                    .mapToDouble(s -> s.getTotal_deduction() != null ? s.getTotal_deduction() : 0)
                    .sum();

            String monthLabel = ym.format(displayFormatter);
            monthLabel = monthLabel.substring(0, 1).toUpperCase() + monthLabel.substring(1);

            Map<String, Double> totals = new HashMap<>();
            totals.put("net", totalNet);
            totals.put("brut", totalBrut);
            totals.put("deduction", totalDeduction);

            // Remplacer les zéros par les vraies valeurs
            monthlyTotals.put(monthLabel, totals);
        }

        // Totaux globaux
        double totalBrutAll = monthlyTotals.values().stream().mapToDouble(m -> m.getOrDefault("brut", 0.0)).sum();
        double totalNetAll = monthlyTotals.values().stream().mapToDouble(m -> m.getOrDefault("net", 0.0)).sum();
        double totalDeductionAll = monthlyTotals.values().stream().mapToDouble(m -> m.getOrDefault("deduction", 0.0))
                .sum();

        String username = (String) session.getAttribute("username");

        model.addAttribute("totalBrutAll", totalBrutAll);
        model.addAttribute("totalNetAll", totalNetAll);
        model.addAttribute("totalDeductionAll", totalDeductionAll);
        model.addAttribute("username", username);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("page", "salary-statistique");

        return "layout/base";
    }

    @GetMapping("/salary-slip")
    @ResponseBody
    public ResponseEntity<SalarySlip> afficherFicheSalaire(@RequestParam String idRef, HttpSession session) {
        System.out.println("appel controller avec idRef = " + idRef);
        SalarySlip slip = salaryService.getSalarySlipByIdRef(session, idRef);
        if (slip != null) {
            return ResponseEntity.ok(slip);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/salary-graphe")
    public String salaryByYear(
            @RequestParam(value = "year", required = false) Integer year,
            Model model,
            HttpSession session) {

        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.trim().isEmpty()) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "salary-graphe");

        if (year == null) {
            year = 2025;
        }

        model.addAttribute("selectedYear", year);

        if (year != null) {
            // 1. Earnings & Deductions détaillés
            Map<String, List<Earning>> earningsByComponent = salaryService.getEarningsDetailsByComponent(session, year);
            Map<String, List<Deduction>> deductionsByComponent = salaryService.getDeductionsDetailsByComponent(session,
                    year);

            // 2. Totaux par composant
            Map<String, Double> earningsTotals = sumAmounts(earningsByComponent, Earning::getAmount);
            Map<String, Double> deductionsTotals = sumAmounts(deductionsByComponent, Deduction::getAmount);

            // 3. Préparer données pour graphiques (earnings)
            List<Map<String, Object>> earningsChartData = new ArrayList<>();
            for (Map.Entry<String, List<Earning>> entry : earningsByComponent.entrySet()) {
                String comp = entry.getKey();
                for (Earning earning : entry.getValue()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("composant", comp);
                    data.put("mois", earning.getYearMonth());
                    data.put("montant", earning.getAmount());
                    earningsChartData.add(data);
                }
            }

            // 4. Préparer données pour graphiques (deductions)
            List<Map<String, Object>> deductionsChartData = new ArrayList<>();
            for (Map.Entry<String, List<Deduction>> entry : deductionsByComponent.entrySet()) {
                String comp = entry.getKey();
                for (Deduction deduction : entry.getValue()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("composant", comp);
                    data.put("mois", deduction.getYearMonth());
                    data.put("montant", deduction.getAmount());
                    deductionsChartData.add(data);
                }
            }

            // 5. Injecter dans le modèle
            model.addAttribute("earningsByComponent", earningsByComponent);
            model.addAttribute("deductionsByComponent", deductionsByComponent);
            model.addAttribute("earningsTotals", earningsTotals);
            model.addAttribute("deductionsTotals", deductionsTotals);
            model.addAttribute("earningsChartData", earningsChartData);
            model.addAttribute("deductionsChartData", deductionsChartData);
            model.addAttribute("selectedYear", year);
        }

        return "layout/base";
    }

    /**
     * Méthode générique pour sommer les montants d’une map de listes d’objets ayant
     * un montant.
     * 
     * @param dataMap         Map avec clé String et valeur List<T>
     * @param amountExtractor Fonction lambda pour extraire le montant de l'objet T
     * @param <T>             Type générique de l'objet (Earning ou Deduction)
     * @return Map des totaux par clé
     */
    private <T> Map<String, Double> sumAmounts(Map<String, List<T>> dataMap, ToDoubleFunction<T> amountExtractor) {
        Map<String, Double> sums = new HashMap<>();
        if (dataMap != null) {
            for (Map.Entry<String, List<T>> entry : dataMap.entrySet()) {
                double total = entry.getValue().stream()
                        .filter(Objects::nonNull)
                        .mapToDouble(amountExtractor)
                        .sum();
                sums.put(entry.getKey(), total);
            }
        }
        return sums;
    }

    @GetMapping("/salary-add-salary")
    public String AddSalary(Model model, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "salary-add-salary");

        List<Employee> employees = employeeService.getAllEmployees(session);
        List<Company> company = companyService.getAllCompanies(session);
        List<BaseSalary> structure = baseSalaryService.getAllBaseSalaries(session);

        model.addAttribute("employees", employees); // Injecter dans le modèle
        model.addAttribute("company", company); // Injecter dans le modèle
        model.addAttribute("structure", structure); // Injecter dans le modèle

        return "layout/base";
    }

    @PostMapping("/salary-add-salary")
    public String genererSalaire(
            @RequestParam("ref") String employeRef,
            @RequestParam("dateDebut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam("dateFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam("company") String company,
            @RequestParam("salaryStructure") String salaryStructure,
            @RequestParam(name = "montant", required = false, defaultValue = "0.0") Double montant,
            HttpSession session,
            @RequestParam(name = "operation", required = false) String action,
            RedirectAttributes redirectAttributes) {

        boolean success = baseSalaryService.regenererSalaireAvecEcraser(
                employeRef,
                dateDebut.toString(),
                dateFin.toString(),
                company,
                salaryStructure,
                montant,
                action,
                session);

                System.out.println("➡️ Valeur de 'operation' reçue : " + action);


        if (success) {
            redirectAttributes.addFlashAttribute("message", "✅ Salaire généré avec succès.");
        } else {
            redirectAttributes.addFlashAttribute("error", "❌ Échec lors de la génération du salaire.");
        }

        return "redirect:/salary-add-salary"; // 🔁 Redirige vers le formulaire au lieu de "layout/base"
    }

    @GetMapping("/salary-update")
    public String UpDateSalary(Model model, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "salary-update");

        List<Component> component = componentService.getAllComponents(session);

        model.addAttribute("component", component);

        return "layout/base";
    }

    @PostMapping("/salary-update")
    public String postUpdateSalary(
            @RequestParam("component") String componentName,
            @RequestParam("condition") String condition,
            @RequestParam("value") double value,
            @RequestParam("percentage") double percentage,
            @RequestParam("operation") String action,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            componentService.regenererSalaire(componentName, condition, value, percentage, action, session);
            redirectAttributes.addFlashAttribute("message", "✅ Salaire modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌Erreur lors de la modification du salaire.");
        }

        return "redirect:/salary-update";
    }

    @GetMapping("/salary/delete")
    public String deleteEmployee(
            @RequestParam("name") String name,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            salaryService.deleteSalaryByName(name, session);
            redirectAttributes.addFlashAttribute("successMessage", "Salaire supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression : " + e.getMessage());
        }

        return "redirect:/salary-employe";
    }

    @GetMapping("/recherche-salaire")
    public String RechercheSalary(Model model, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "recherche-salaire");

        List<Component> component = componentService.getAllComponents(session);

        model.addAttribute("component", component); // Injecter dans le modèle

        return "layout/base";
    }

    @PostMapping("/recherche-salaire")
    public String RecherchesSalary(
            @RequestParam("component") String componentName,
            @RequestParam("condition") String condition,
            @RequestParam("value") double value,
            Model model,
            HttpSession session) {

        try {
            List<SalarySlip> resultat = componentService.rechercherSalaireEtRetourner(
                    condition, value, componentName, session);

            model.addAttribute("resultat", resultat);
            model.addAttribute("message", "✅ Salaire récupéré avec succès.");
        } catch (Exception e) {
            model.addAttribute("error", "❌Erreur lors de la récupération du salaire.");
        }

        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("page", "recherche-salaire");

        List<Component> component = componentService.getAllComponents(session);
        model.addAttribute("component", component);

        return "layout/base";
    }

}
