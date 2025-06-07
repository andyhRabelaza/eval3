package com.spring.erpnext.service;

import com.spring.erpnext.model.Deduction;
import com.spring.erpnext.model.Earning;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

}
