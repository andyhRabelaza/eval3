package com.spring.erpnext.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryStructureAssignment {

    private String name; // identifiant unique de l'assignation
    private String employee;
    private String salary_structure;
    private String assigned_date; // si tu veux gérer la date d'assignation

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

    public String getSalary_structure() {
        return salary_structure;
    }

    public void setSalary_structure(String salary_structure) {
        this.salary_structure = salary_structure;
    }

    public String getAssigned_date() {
        return assigned_date;
    }

    public void setAssigned_date(String assigned_date) {
        this.assigned_date = assigned_date;
    }
}
