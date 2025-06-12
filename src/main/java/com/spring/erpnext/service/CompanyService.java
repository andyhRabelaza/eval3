package com.spring.erpnext.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.spring.erpnext.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Service
public class CompanyService {

    private static final String BASE_URL = "http://erpnext.localhost:8000";

    @Autowired
    private RestTemplate restTemplate;

    public List<Company> getAllCompanies(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String url = BASE_URL + "/api/resource/Company?fields=[\"name\"]&limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<CompanyApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                CompanyApiResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur lors de la récupération des compagnies.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompanyApiResponse {
        private List<Company> data;

        public List<Company> getData() {
            return data;
        }

        public void setData(List<Company> data) {
            this.data = data;
        }
    }
}
