package com.spring.erpnext.controller;

import com.spring.erpnext.model.SalarySlip;
import com.spring.erpnext.service.SalaryService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Controller
public class SalaryController {

    private final SalaryService salaryService;

    @Autowired
    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
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

        model.addAttribute("salarySlips", paginatedSalaries);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("page", "salary");

        return "layout/base";
    }

    @GetMapping("/salaremp")
    public String filtreEmployeEtSalaire(
            @RequestParam(value = "monthYear", required = false) String monthYear,
            Model model,
            HttpSession session) {

        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            return "redirect:/login";
        }

        List<SalarySlip> salarySlips;

        Integer year = null;
        Integer month = null;
        if (monthYear != null && !monthYear.isEmpty()) {
            String[] parts = monthYear.split("-");
            if (parts.length == 2) {
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
            }
        }

        if (year != null && month != null) {
            salarySlips = salaryService.getSalariesByMonth(session, year, month);
            model.addAttribute("selectedMonth", monthYear);
        } else {
            salarySlips = salaryService.getAllSalaries(session);
        }

        double totalGrossPay = salarySlips.stream()
                .filter(slip -> slip.getGross_pay() != null)
                .mapToDouble(SalarySlip::getGross_pay)
                .sum();

        double totalNetPay = salarySlips.stream()
                .filter(slip -> slip.getNet_pay() != null)
                .mapToDouble(SalarySlip::getNet_pay)
                .sum();

        model.addAttribute("salarySlips", salarySlips);
        model.addAttribute("totalGrossPay", totalGrossPay);
        model.addAttribute("totalNetPay", totalNetPay);
        model.addAttribute("page", "salaremp");

        return "layout/base";
    }

    @GetMapping("/salary/export/pdf")
    public void exportSalaryPdf(String name, HttpSession session, HttpServletResponse response)
            throws IOException, DocumentException {

        SalarySlip salarySlip = salaryService.getSalaryByName(name, session);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=salary_" + name + ".pdf");

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // Couleurs
        BaseColor darkBlue = new BaseColor(10, 51, 99);
        BaseColor lightGray = new BaseColor(230, 230, 230);
        BaseColor white = BaseColor.WHITE;

        // Polices
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, darkBlue);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, white);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, darkBlue);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 12);

        // Titre centré
        Paragraph title = new Paragraph("Bulletin de salaire", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(25);
        document.add(title);

        // Table info générale
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 3, 7 });
        table.setSpacingAfter(30);

        // Cellules d'en-tête colorées (exemple)
        PdfPCell headerCell = new PdfPCell(new Phrase("Informations générales", headerFont));
        headerCell.setColspan(2);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBackgroundColor(darkBlue);
        headerCell.setPadding(10);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setUseVariableBorders(true);
        table.addCell(headerCell);

        // Méthode utilitaire pour créer des cellules label + valeur avec styles
        BiFunction<String, Font, PdfPCell> createLabelCell = (text, font) -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(8);
            return cell;
        };
        BiFunction<String, Font, PdfPCell> createValueCell = (text, font) -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(8);
            return cell;
        };

        // Ajout des données info générales
        table.addCell(createLabelCell.apply("Nom du bulletin", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getName(), valueFont));

        table.addCell(createLabelCell.apply("Employé", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getEmployee_name(), valueFont));

        table.addCell(createLabelCell.apply("Période", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getStart_date() + " au " + salarySlip.getEnd_date(), valueFont));

        table.addCell(createLabelCell.apply("Date de publication", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getPosting_date(), valueFont));

        table.addCell(createLabelCell.apply("Structure salariale", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getSalary_structure(), valueFont));

        table.addCell(createLabelCell.apply("Société", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getCompany(), valueFont));

        table.addCell(createLabelCell.apply("Statut", labelFont));
        table.addCell(createValueCell.apply(salarySlip.getStatus(), valueFont));

        document.add(table);

        // Table détail salaire
        PdfPTable salaryTable = new PdfPTable(2);
        salaryTable.setWidthPercentage(50);
        salaryTable.setWidths(new int[] { 4, 3 });
        salaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        // En-tête table salaire
        PdfPCell salaryHeaderCell = new PdfPCell(new Phrase("Détails de la paie", headerFont));
        salaryHeaderCell.setColspan(2);
        salaryHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        salaryHeaderCell.setBackgroundColor(darkBlue);
        salaryHeaderCell.setPadding(10);
        salaryHeaderCell.setBorder(Rectangle.NO_BORDER);
        salaryTable.addCell(salaryHeaderCell);

        // Méthode pour cellule salaire avec fond alterné
        Function<String, PdfPCell> createSalaryLabelCell = text -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, labelFont));
            cell.setBackgroundColor(lightGray);
            cell.setPadding(8);
            return cell;
        };

        Function<String, PdfPCell> createSalaryValueCell = text -> {
            PdfPCell cell = new PdfPCell(new Phrase(text, valueFont));
            cell.setPadding(8);
            return cell;
        };

        salaryTable.addCell(createSalaryLabelCell.apply("Salaire brut (" + salarySlip.getCurrency() + ")"));
        salaryTable.addCell(createSalaryValueCell.apply(String.format("%.2f", salarySlip.getGross_pay())));

        salaryTable.addCell(createSalaryLabelCell.apply("Salaire net (" + salarySlip.getCurrency() + ")"));
        salaryTable.addCell(createSalaryValueCell.apply(String.format("%.2f", salarySlip.getNet_pay())));

        salaryTable.addCell(createSalaryLabelCell.apply("Jours payés"));
        salaryTable.addCell(createSalaryValueCell.apply(String.valueOf(salarySlip.getPayment_days())));

        salaryTable.addCell(createSalaryLabelCell.apply("Congés sans solde"));
        salaryTable.addCell(createSalaryValueCell.apply(String.valueOf(salarySlip.getLeave_without_pay())));

        document.add(salaryTable);

        // Pied de page
        Paragraph footer = new Paragraph("\nMerci pour votre travail au sein de " + salarySlip.getCompany(), valueFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(50);
        document.add(footer);

        document.close();
    }

}
