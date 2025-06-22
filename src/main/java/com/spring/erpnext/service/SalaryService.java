package com.spring.erpnext.service;

import com.spring.erpnext.model.Deduction;
import com.spring.erpnext.model.Earning;
import com.spring.erpnext.model.Employee;
import com.spring.erpnext.model.SalarySlip;
import com.spring.erpnext.model.SalaryStructureAssignment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SalaryService {

    private final RestTemplate restTemplate;

    private final String BASE_URL = "http://erpnext.localhost:8000";

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public List<String> getSalarySlipsNamesByYear(HttpSession session, int year) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        String startOfYear = String.format("%04d-01-01", year);
        String endOfYear = String.format("%04d-12-31", year);

        String filtersJson = "[[\"start_date\", \">=\", \"" + startOfYear + "\"]," +
                "[\"end_date\", \"<=\", \"" + endOfYear + "\"]]";

        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://erpnext.localhost:8000/api/resource/Salary Slip")
                .queryParam("fields", "[\"name\", \"start_date\"]")
                .queryParam("filters", filtersJson)
                .queryParam("sid", sid)
                .queryParam("limit_page_length", "1000")
                .build()
                .encode()
                .toUri();

        System.out.println("Filtres JSON : " + filtersJson);
        System.out.println("URL construite : " + uri);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SalaryApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    SalaryApiResponse.class);

            System.out.println("Corps de la réponse : " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<SalarySlip> slips = response.getBody().getData();
                // Extraire la liste des noms
                System.out.println("Nombre de bulletins récupérés : " + slips.size());
                return slips.stream().map(SalarySlip::getName).collect(Collectors.toList());
            } else {
                throw new RuntimeException("Erreur lors de la récupération des bulletins de salaire.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'appel à l'API Frappe ERPNext.");
        }
    }

    public SalarySlip getDetailsForSalarySlip(HttpSession session, String salarySlipName) {
        String sid = (String) session.getAttribute("sid");
        System.out.println("[DEBUG] SID depuis la session: " + sid);
        System.out.println("[DEBUG] Nom du bulletin demandé: " + salarySlipName);

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://erpnext.localhost:8000/api/resource/Salary Slip/" + salarySlipName)
                .queryParam("fields", "[\"*\"]")
                .build()
                .encode()
                .toUri();

        System.out.println("[DEBUG] URI construite pour récupérer le bulletin: " + uri);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid); // Auth via cookie si nécessaire

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SalarySlipApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    SalarySlipApiResponse.class);

            System.out.println("[DEBUG] Statut HTTP reçu: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SalarySlip slip = response.getBody().getData();
                System.out.println("[DEBUG] Bulletin trouvé: " + (slip != null ? slip.getName() : "null"));

                if (slip != null) {
                    String startDate = slip.getStart_date(); // ex: "2025-06-01"
                    if (startDate != null && startDate.length() >= 7) {
                        String yearMonth = startDate.substring(0, 7); // "YYYY-MM"
                        System.out.println("[DEBUG] Mois/Année du bulletin : " + yearMonth);
                    } else {
                        System.out.println("[DEBUG] Mois/Année du bulletin : inconnu");
                    }
                    return slip;
                } else {
                    throw new RuntimeException("Aucun détail trouvé pour le bulletin : " + salarySlipName);
                }
            } else {
                throw new RuntimeException("Réponse invalide pour le bulletin " + salarySlipName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'appel à l'API Frappe ERPNext pour " + salarySlipName);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SalarySlipApiResponse {
        private SalarySlip data;

        public SalarySlip getData() {
            return data;
        }

        public void setData(SalarySlip data) {
            this.data = data;
        }
    }

    public Map<String, List<Earning>> getEarningsDetailsByComponent(HttpSession session, int year) {
        List<String> salarySlipNames = getSalarySlipsNamesByYear(session, year);
        Map<String, List<Earning>> earningsByComponent = new HashMap<>();

        for (String slipName : salarySlipNames) {
            SalarySlip slip = getDetailsForSalarySlip(session, slipName);
            if (slip == null || slip.getEarnings() == null) {
                continue; // Pas d'earnings pour ce bulletin
            }

            String yearMonth = slip.getYearMonth();
            System.out.println("[INFO] Bulletin " + slip.getName() + " - Mois/Année: " + yearMonth);

            for (Earning earning : slip.getEarnings()) {
                earning.setYearMonth(yearMonth); // <-- on renseigne yearMonth ici

                String compName = earning.getSalary_component();
                if (compName == null)
                    continue; // ignore si composant null

                earningsByComponent
                        .computeIfAbsent(compName, k -> new ArrayList<>())
                        .add(earning);
            }
        }

        // Ajout du résumé des earnings par mois et composant
        System.out.println("\n=== [Résumé des earnings par composant et par mois] ===");
        for (Map.Entry<String, List<Earning>> entry : earningsByComponent.entrySet()) {
            String component = entry.getKey();
            Map<String, Double> totalByMonth = new HashMap<>();

            for (Earning earning : entry.getValue()) {
                String month = earning.getYearMonth();
                double amount = earning.getAmount(); // Supposé retourner un double
                totalByMonth.put(month, totalByMonth.getOrDefault(month, 0.0) + amount);
            }

            for (Map.Entry<String, Double> monthEntry : totalByMonth.entrySet()) {
                System.out.println("Composant: " + component + " | Mois: " + monthEntry.getKey() + " | Total: "
                        + monthEntry.getValue());
            }
        }

        return earningsByComponent;
    }

    public Map<String, List<Deduction>> getDeductionsDetailsByComponent(HttpSession session, int year) {
        List<String> salarySlipNames = getSalarySlipsNamesByYear(session, year);
        Map<String, List<Deduction>> deductionsByComponent = new HashMap<>();

        for (String slipName : salarySlipNames) {
            SalarySlip slip = getDetailsForSalarySlip(session, slipName);
            if (slip == null || slip.getDeductions() == null) {
                continue; // Pas de deductions pour ce bulletin
            }

            String yearMonth = slip.getYearMonth();
            System.out.println("[INFO] Bulletin " + slip.getName() + " - Mois/Année: " + yearMonth);

            for (Deduction deduction : slip.getDeductions()) {
                deduction.setYearMonth(yearMonth); // <-- on renseigne yearMonth ici

                String compName = deduction.getSalary_component();
                if (compName == null)
                    continue; // ignore si composant null

                deductionsByComponent
                        .computeIfAbsent(compName, k -> new ArrayList<>())
                        .add(deduction);
            }
        }

        // Ajout du résumé des deductions par mois et composant
        System.out.println("\n=== [Résumé des deductions par composant et par mois] ===");
        for (Map.Entry<String, List<Deduction>> entry : deductionsByComponent.entrySet()) {
            String component = entry.getKey();
            Map<String, Double> totalByMonth = new HashMap<>();

            for (Deduction deduction : entry.getValue()) {
                String month = deduction.getYearMonth();
                double amount = deduction.getAmount(); // Supposé retourner un double
                totalByMonth.put(month, totalByMonth.getOrDefault(month, 0.0) + amount);
            }

            for (Map.Entry<String, Double> monthEntry : totalByMonth.entrySet()) {
                System.out.println("Composant: " + component + " | Mois: " + monthEntry.getKey() + " | Total: "
                        + monthEntry.getValue());
            }
        }

        return deductionsByComponent;
    }

    public void deleteAllAssignments(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        // 1. Récupérer toutes les assignations avec leur docstatus
        String urlGet = "http://erpnext.localhost:8000/api/resource/Salary Structure Assignment?fields=[\"name\", \"docstatus\"]&limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<AssignmentApiResponse> response = restTemplate.exchange(
                urlGet,
                HttpMethod.GET,
                entity,
                AssignmentApiResponse.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Erreur lors de la récupération des assignations.");
        }

        List<SalaryStructureAssignment> assignments = response.getBody().getData();

        for (SalaryStructureAssignment assignment : assignments) {
            String name = assignment.getName();

            // Étape 2 : Annuler l’assignation (docstatus = 2)
            String cancelUrl = "http://erpnext.localhost:8000/api/resource/Salary Structure Assignment/" + name;
            Map<String, Object> cancelPayload = new HashMap<>();
            cancelPayload.put("docstatus", 2);

            HttpHeaders cancelHeaders = new HttpHeaders();
            cancelHeaders.setContentType(MediaType.APPLICATION_JSON);
            cancelHeaders.set("Cookie", "sid=" + sid);
            HttpEntity<Map<String, Object>> cancelEntity = new HttpEntity<>(cancelPayload, cancelHeaders);

            try {
                restTemplate.exchange(
                        cancelUrl,
                        HttpMethod.PUT,
                        cancelEntity,
                        String.class);
            } catch (Exception e) {
                System.out.println("Erreur lors de l'annulation de l'assignation : " + name);
                e.printStackTrace();
                continue; // Passer au suivant si annulation échoue
            }

            // Étape 3 : Supprimer
            try {
                HttpHeaders deleteHeaders = new HttpHeaders();
                deleteHeaders.set("Cookie", "sid=" + sid);
                HttpEntity<String> deleteEntity = new HttpEntity<>(deleteHeaders);

                ResponseEntity<String> deleteResponse = restTemplate.exchange(
                        cancelUrl,
                        HttpMethod.DELETE,
                        deleteEntity,
                        String.class);

                if (deleteResponse.getStatusCode() != HttpStatus.OK) {
                    System.out.println("Erreur suppression assignation : " + name);
                }
            } catch (Exception e) {
                System.out.println("Exception suppression assignation : " + name);
                e.printStackTrace();
            }
        }
    }

    // Classe interne pour la réponse de l'API
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssignmentApiResponse {
        private List<SalaryStructureAssignment> data;

        public List<SalaryStructureAssignment> getData() {
            return data;
        }

        public void setData(List<SalaryStructureAssignment> data) {
            this.data = data;
        }
    }

    public void deleteAllSalarySlips(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        // 1. Récupérer tous les SalarySlip (champ 'name')
        String urlGet = "http://erpnext.localhost:8000/api/resource/Salary Slip?fields=[\"name\"]&limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<SalaryApiResponse> response = restTemplate.exchange(
                urlGet,
                HttpMethod.GET,
                entity,
                SalaryApiResponse.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Erreur lors de la récupération des Salary Slips.");
        }

        List<SalarySlip> slips = response.getBody().getData();

        for (SalarySlip slip : slips) {
            String slipName = slip.getName();

            // 2. Annuler immédiatement (on suppose docstatus = 1)
            String cancelUrl = "http://erpnext.localhost:8000/api/resource/Salary Slip/" + slipName;

            Map<String, Object> cancelPayload = new HashMap<>();
            cancelPayload.put("docstatus", 2); // 2 = Canceled

            HttpHeaders cancelHeaders = new HttpHeaders();
            cancelHeaders.setContentType(MediaType.APPLICATION_JSON);
            cancelHeaders.set("Cookie", "sid=" + sid);

            HttpEntity<Map<String, Object>> cancelEntity = new HttpEntity<>(cancelPayload, cancelHeaders);

            try {
                restTemplate.exchange(
                        cancelUrl,
                        HttpMethod.PUT,
                        cancelEntity,
                        String.class);
            } catch (Exception e) {
                System.out.println("Erreur lors de l'annulation de Salary Slip : " + slipName);
                e.printStackTrace();
                continue;
            }

            // 3. Supprimer
            String deleteUrl = "http://erpnext.localhost:8000/api/resource/Salary Slip/" + slipName;
            try {
                HttpHeaders deleteHeaders = new HttpHeaders();
                deleteHeaders.set("Cookie", "sid=" + sid);
                HttpEntity<String> deleteEntity = new HttpEntity<>(deleteHeaders);

                ResponseEntity<String> deleteResponse = restTemplate.exchange(
                        deleteUrl,
                        HttpMethod.DELETE,
                        deleteEntity,
                        String.class);

                if (deleteResponse.getStatusCode() != HttpStatus.OK) {
                    System.out.println("Erreur suppression Salary Slip : " + slipName);
                }
            } catch (Exception e) {
                System.out.println("Exception suppression Salary Slip : " + slipName);
                e.printStackTrace();
            }
        }
    }

    public void deleteAllEmployees(HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Aucune session 'sid' trouvée.");
        }

        // 1. Récupérer tous les employés (seulement le champ 'name')
        String urlGet = "http://erpnext.localhost:8000/api/resource/Employee?fields=[\"name\"]&limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<EmployeeApiResponse> response = restTemplate.exchange(
                urlGet,
                HttpMethod.GET,
                entity,
                EmployeeApiResponse.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Erreur lors de la récupération des employés.");
        }

        List<Employee> employees = response.getBody().getData();

        // 2. Supprimer tous les employés un par un
        for (Employee employee : employees) {
            String deleteUrl = "http://erpnext.localhost:8000/api/resource/Employee/" + employee.getName();
            try {
                HttpHeaders deleteHeaders = new HttpHeaders();
                deleteHeaders.set("Cookie", "sid=" + sid);
                HttpEntity<String> deleteEntity = new HttpEntity<>(deleteHeaders);

                ResponseEntity<String> deleteResponse = restTemplate.exchange(
                        deleteUrl,
                        HttpMethod.DELETE,
                        deleteEntity,
                        String.class);

                if (deleteResponse.getStatusCode() != HttpStatus.OK) {
                    System.out.println("Erreur suppression employé : " + employee.getName());
                }
            } catch (Exception e) {
                System.out.println("Exception suppression employé : " + employee.getName());
                e.printStackTrace();
            }
        }
    }

    // Classe interne pour la réponse de l'API
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

    public void deleteSalaryByName(String name, HttpSession session) throws Exception {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            throw new IllegalStateException("Session expirée ou non valide.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 1. Récupérer le Salary Slip
        String salarySlipUrl = BASE_URL + "/api/resource/Salary Slip/" + name;

        ResponseEntity<SalarySlipResponse> slipResponse = restTemplate.exchange(
                salarySlipUrl, HttpMethod.GET, entity, SalarySlipResponse.class);

        SalarySlip slip = slipResponse.getBody() != null ? slipResponse.getBody().getData() : null;

        if (slip == null) {
            throw new RuntimeException("Impossible de récupérer le Salary Slip.");
        }

        String employee = slip.getEmployee();
        String salaryStructure = slip.getSalary_structure();
        String fromDate = slip.getStart_date();

        System.out.println("🔎 Recherche SSA pour:");
        System.out.println("Employee: " + employee);
        System.out.println("Salary Structure: " + salaryStructure);
        System.out.println("From Date: " + fromDate);

        // 2. Annuler le Salary Slip
        String cancelPayload = "{\"docstatus\": 2}";
        HttpEntity<String> cancelEntity = new HttpEntity<>(cancelPayload, headers);

        ResponseEntity<String> cancelResponse = restTemplate.exchange(
                salarySlipUrl, HttpMethod.PUT, cancelEntity, String.class);

        if (!cancelResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Échec de l'annulation du Salary Slip : " + cancelResponse.getBody());
        }

        // 3. Supprimer le Salary Slip
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                salarySlipUrl, HttpMethod.DELETE, entity, String.class);

        if (!deleteResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Échec de la suppression du Salary Slip : " + deleteResponse.getBody());
        }

        // 4. Rechercher et supprimer le Salary Structure Assignment
        if (employee != null && salaryStructure != null && fromDate != null) {
            String filters = "[[\"employee\", \"=\", \"" + employee + "\"]," +
                    "[\"salary_structure\", \"=\", \"" + salaryStructure + "\"]," +
                    "[\"from_date\", \"=\", \"" + fromDate + "\"]]";

            String fields = "[\"name\"]";

            String filterUrl = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + "/api/resource/Salary Structure Assignment")
                    .queryParam("filters", filters)
                    .queryParam("fields", fields)
                    .build()
                    .toUriString();

            ResponseEntity<String> assignResponse = restTemplate.exchange(
                    filterUrl, HttpMethod.GET, entity, String.class);

            JsonNode assignRoot = objectMapper.readTree(assignResponse.getBody());
            JsonNode data = assignRoot.get("data");

            if (data != null && data.isArray() && data.size() > 0) {
                String assignName = data.get(0).get("name").asText();
                System.out.println("✅ Salary Structure Assignment trouvé : " + assignName);

                String cancelAssignUrl = BASE_URL + "/api/resource/Salary Structure Assignment/" +
                        UriUtils.encodePathSegment(assignName, StandardCharsets.UTF_8);

                HttpEntity<String> cancelAssignEntity = new HttpEntity<>("{\"docstatus\": 2}", headers);

                ResponseEntity<String> cancelSSAResponse = restTemplate.exchange(
                        cancelAssignUrl, HttpMethod.PUT, cancelAssignEntity, String.class);

                if (!cancelSSAResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Échec de l'annulation du SSA : " + cancelSSAResponse.getBody());
                }

                restTemplate.exchange(cancelAssignUrl, HttpMethod.DELETE, entity, String.class);
            } else {
                System.out.println("❌ Aucun Salary Structure Assignment trouvé.");
            }
        }
    }

}
