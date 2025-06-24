package com.spring.erpnext.service;

import com.spring.erpnext.model.Employee;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

@Service
public class TestService {

    private final JdbcTemplate jdbcTemplate;

    public TestService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countEmployees() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tabEmployee", Integer.class);
    }

    public String testConnection() {
        return jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
    }

    /// 🔹 Méthode pour récupérer tous les employés
    public List<Employee> getAllEmployees() {
        String sql = "SELECT name, first_name, middle_name, last_name, date_of_birth, date_of_joining, gender, company, status FROM tabEmployee";

        return jdbcTemplate.query(sql, new RowMapper<Employee>() {
            @Override
            public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
                Employee emp = new Employee();
                emp.setName(rs.getString("name"));
                emp.setFirst_name(rs.getString("first_name"));
                emp.setMiddle_name(rs.getString("middle_name"));
                emp.setLast_name(rs.getString("last_name"));
                emp.setDate_of_birth(rs.getString("date_of_birth"));
                emp.setDate_of_joining(rs.getString("date_of_joining"));
                emp.setGender(rs.getString("gender"));
                emp.setCompany(rs.getString("company"));
                emp.setStatus(rs.getString("status"));
                return emp;
            }
        });
    }

}
