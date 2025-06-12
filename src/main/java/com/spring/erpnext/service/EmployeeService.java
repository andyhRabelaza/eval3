package com.spring.erpnext.service;

import com.spring.erpnext.model.Employee;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Service
public class EmployeeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmployeeService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Employee> getAllEmployees(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/resource/Employee" +
                "?fields=[\"name\",\"last_name\",\"middle_name\",\"first_name\",\"date_of_birth\",\"date_of_joining\",\"gender\",\"company\",\"status\"]"
                +
                "&limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<EmployeeApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                EmployeeApiResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur lors de la récupération des employés.");
        }
    }

    public List<Employee> getAllEmployee(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/resource/Employee" +
                "?fields=[\"name\"]"
                +
                "&limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<EmployeeApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                EmployeeApiResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur lors de la récupération des employés.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmployeeApiResponse {
        private List<Employee> data;

        public List<Employee> getData() {
            return data;
        }

        public void setData(List<Employee> data) {
            this.data = data;
        }
    }

    public int getEmployeeCount(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/method/frappe.client.get_count";

        String body = "{\"doctype\": \"Employee\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", "sid=" + sid);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<EmployeeCountResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                EmployeeCountResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getMessage();
        } else {
            throw new RuntimeException("Erreur lors de la récupération du nombre d'employés.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmployeeCountResponse {
        private int message;

        public int getMessage() {
            return message;
        }

        public void setMessage(int message) {
            this.message = message;
        }
    }

    public boolean deleteEmployee(String employeeId, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/resource/Employee/" + employeeId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class);

            return response.getStatusCode() == HttpStatus.NO_CONTENT || response.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException e) {
            System.err.println(
                    "Erreur lors de la suppression : " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        }
    }

    public boolean insertEmployee(Employee employee, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/resource/Employee";

        try {
            String employeeJson = objectMapper.writeValueAsString(employee);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", "sid=" + sid);

            HttpEntity<String> entity = new HttpEntity<>(employeeJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            return response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
