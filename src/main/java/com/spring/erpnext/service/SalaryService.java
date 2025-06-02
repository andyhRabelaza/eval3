package com.spring.erpnext.service;

import com.spring.erpnext.model.SalarySlip;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Service
public class SalaryService {

    private final RestTemplate restTemplate;

    public SalaryService() {
        this.restTemplate = new RestTemplate();
    }

    public List<SalarySlip> getAllSalaries(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/resource/Salary Slip" +
                "?fields=[\"name\",\"employee\",\"employee_name\",\"start_date\",\"end_date\",\"gross_pay\",\"net_pay\",\"payment_days\",\"leave_without_pay\",\"salary_structure\",\"posting_date\",\"company\",\"status\",\"currency\"]"
                +
                "&limit_page_length=1000";

        // Prépare les headers avec le cookie 'sid'
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<SalaryApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SalaryApiResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur lors de la récupération des bulletins de salaire.");
        }
    }

    // Classe interne pour mapper la réponse JSON
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SalaryApiResponse {
        private List<SalarySlip> data;

        public List<SalarySlip> getData() {
            return data;
        }

        public void setData(List<SalarySlip> data) {
            this.data = data;
        }
    }

    public SalarySlip getSalaryByName(String name, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = "http://erpnext.localhost:8000/api/resource/Salary Slip/" + name;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<SalarySlipResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SalarySlipResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur lors de la récupération du bulletin de salaire : " + name);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SalarySlipResponse {
        private SalarySlip data;

        public SalarySlip getData() {
            return data;
        }

        public void setData(SalarySlip data) {
            this.data = data;
        }
    }

}
