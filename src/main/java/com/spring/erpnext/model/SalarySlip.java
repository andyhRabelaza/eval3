package com.spring.erpnext.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalarySlip {

    private String name;
    private String employee;
    private String employee_name;
    private String start_date;
    private String end_date;
    private Double gross_pay;
    private Double net_pay;
    private Integer payment_days;
    private Integer leave_without_pay;
    private String salary_structure;
    private String posting_date;
    private String company;
    private String status;
    private String currency;

    @JsonProperty("earnings") // adapte au vrai nom dans JSON si nécessaire
    private List<Earning> earnings;

    @JsonProperty("deductions") // adapte au vrai nom dans JSON si nécessaire
    private List<Deduction> deductions;

    private Double total_deduction;

    // --- Getters & Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getEmployee_name() {
        return employee_name;
    }

    public void setEmployee_name(String employee_name) {
        this.employee_name = employee_name;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getStartDateMonth() {
        if (start_date == null || start_date.isEmpty()) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(start_date, formatter);
        return date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE);
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public Double getGross_pay() {
        return gross_pay;
    }

    public void setGross_pay(Double gross_pay) {
        this.gross_pay = gross_pay;
    }

    public Double getNet_pay() {
        return net_pay;
    }

    public void setNet_pay(Double net_pay) {
        this.net_pay = net_pay;
    }

    public Integer getPayment_days() {
        return payment_days;
    }

    public void setPayment_days(Integer payment_days) {
        this.payment_days = payment_days;
    }

    public Integer getLeave_without_pay() {
        return leave_without_pay;
    }

    public void setLeave_without_pay(Integer leave_without_pay) {
        this.leave_without_pay = leave_without_pay;
    }

    public String getSalary_structure() {
        return salary_structure;
    }

    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }

    public String getPosting_date() {
        return posting_date;
    }

    public void setPosting_date(String posting_date) {
        this.posting_date = posting_date;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<Earning> getEarnings() {
        return earnings;
    }

    public void setEarnings(List<Earning> earnings) {
        this.earnings = earnings;
    }

    public List<Deduction> getDeductions() {
        return deductions;
    }

    public void setDeductions(List<Deduction> deductions) {
        this.deductions = deductions;
    }

    public double getDeductionTotal() {
        if (deductions == null)
            return 0.0;
        return deductions.stream()
                .filter(d -> d.getAmount() != null)
                .mapToDouble(Deduction::getAmount)
                .sum();
    }

    public double getEarningTotal() {
        if (earnings == null)
            return 0.0;
        return earnings.stream()
                .filter(e -> e.getAmount() != null)
                .mapToDouble(Earning::getAmount)
                .sum();
    }

    public Double getTotal_deduction() {
        return total_deduction;
    }

    public void setTotal_deduction(Double total_deduction) {
        this.total_deduction = total_deduction;
    }

    public String getYearMonth() {
        if (start_date != null && start_date.length() >= 7) {
            return start_date.substring(0, 7); // "YYYY-MM"
        }
        return "inconnu";
    }

}
