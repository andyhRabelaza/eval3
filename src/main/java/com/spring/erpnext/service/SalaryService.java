package com.spring.erpnext.service;

import com.spring.erpnext.model.SalarySlip;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpSession;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
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
                "?fields=[\"*\"]" +
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

    public List<SalarySlip> getSalariesByMonth(HttpSession session, int year, int month) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String startDate = String.format("%04d-%02d-01", year, month);
        String endDate = String.format("%04d-%02d-%02d", year, month, YearMonth.of(year, month).lengthOfMonth());

        String filtersJson = "[[\"Salary Slip\",\"start_date\",\"<=\",\"" + endDate + "\"]," +
                "[\"Salary Slip\",\"end_date\",\">=\",\"" + startDate + "\"]]";

        System.out.println("SID récupéré depuis la session: " + sid);
        System.out.println("Date de début : " + startDate);
        System.out.println("Date de fin : " + endDate);
        System.out.println("Filtres JSON : " + filtersJson);

        // Construction de l’URL avec le sid en paramètre
        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://erpnext.localhost:8000/api/resource/Salary Slip")
                .queryParam("fields", "[\"*\"]")
                .queryParam("filters", filtersJson)
                .queryParam("sid", sid)
                .queryParam("limit_page_length", "1000")
                .build()
                .encode()
                .toUri();

        System.out.println("URL finale : " + uri);

        HttpHeaders headers = new HttpHeaders();
        // Cookie non requis ici puisque le sid est passé dans l’URL

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SalaryApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    SalaryApiResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Erreur lors de la récupération des bulletins de salaire.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'appel à l'API Frappe ERPNext.");
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

    public SalarySlip getSalarySlipByIdRef(HttpSession session, String idRef) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        // Ne pas encoder manuellement ici
        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://erpnext.localhost:8000/api/resource/Salary Slip/" + idRef)
                .queryParam("fields", "[\"*\"]")
                .queryParam("sid", sid)
                .build()
                .encode() // fait l'encodage correctement une seule fois
                .toUri();

        System.out.println("Appel API pour Salary Slip ID : " + idRef);
        System.out.println("URL finale : " + uri);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SalarySlipResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    SalarySlipResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Erreur lors de la récupération du bulletin de salaire.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'appel à l'API Frappe ERPNext.", e);
        }
    }

    public List<SalarySlip> getSalariesByYear(HttpSession session, int year) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        // Définir les dates de début et de fin pour l'année complète
        String startDate = String.format("%04d-01-01", year);
        String endDate = String.format("%04d-12-31", year);

        String filtersJson = "[[\"Salary Slip\",\"start_date\",\"<=\",\"" + endDate + "\"]," +
                "[\"Salary Slip\",\"end_date\",\">=\",\"" + startDate + "\"]]";

        System.out.println("SID récupéré depuis la session: " + sid);
        System.out.println("Date de début : " + startDate);
        System.out.println("Date de fin : " + endDate);
        System.out.println("Filtres JSON : " + filtersJson);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://erpnext.localhost:8000/api/resource/Salary Slip")
                .queryParam("fields", "[\"*\"]")
                .queryParam("filters", filtersJson)
                .queryParam("sid", sid)
                .queryParam("limit_page_length", "1000")
                .build()
                .encode()
                .toUri();

        System.out.println("URL finale : " + uri);

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SalaryApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    SalaryApiResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Erreur lors de la récupération des bulletins de salaire.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'appel à l'API Frappe ERPNext.");
        }
    }

}
