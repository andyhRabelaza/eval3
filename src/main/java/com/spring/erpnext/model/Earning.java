package com.spring.erpnext.model;

public class Earning {
    private String salary_component;
    private Double amount;
    private String yearMonth;

    public String getSalary_component() {
        return salary_component;
    }

    public void setSalary_component(String salary_component) {
        this.salary_component = salary_component;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public String getYearMonth() {
        return yearMonth;
    }
}
