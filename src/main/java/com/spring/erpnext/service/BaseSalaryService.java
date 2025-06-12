package com.spring.erpnext.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.erpnext.model.BaseSalary;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BaseSalaryService {

    private static final String BASE_URL = "http://erpnext.localhost:8000";

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean insertBaseSalary(BaseSalary baseSalary, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            System.err.println("❌ Session non authentifiée");
            return false;
        }

        try {
            if (!employeeExists(session, baseSalary.getEmployee())) {
                System.err.println("❌ Employé inexistant: " + baseSalary.getEmployee());
                return false;
            }

            String salaryStructure = getDefaultSalaryStructure(session, baseSalary.getCompany());
            if (salaryStructure == null) {
                System.err
                        .println("❌ Aucune structure salariale trouvée pour l'entreprise: " + baseSalary.getCompany());
                return false;
            }

            // 3️⃣ Préparer le payload pour Salary Structure Assignment
            Map<String, Object> payload = new HashMap<>();
            payload.put("employee", baseSalary.getEmployee());
            payload.put("company", baseSalary.getCompany());
            payload.put("from_date", baseSalary.getFrom_Date());
            payload.put("base", baseSalary.getAmount());
            payload.put("salary_structure", salaryStructure);
            payload.put("docstatus", 1); // Soumis

            if (baseSalary.getRemark() != null && !baseSalary.getRemark().trim().isEmpty()) {
                payload.put("remarks", baseSalary.getRemark());
            }

            // 4️⃣ Convertir en JSON
            String json = objectMapper.writeValueAsString(payload);

            System.out.println("📤 Création assignment avec payload: " + json);

            // 5️⃣ Préparer les en-têtes HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", "sid=" + sid);
            headers.set("Expect", ""); // Pour éviter les erreurs 417

            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            // 6️⃣ Envoyer la requête POST
            String url = BASE_URL + "/api/resource/Salary%20Structure%20Assignment";

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // 7️⃣ Vérifier la réponse
            if (response.getStatusCode() == HttpStatus.OK ||
                    response.getStatusCode() == HttpStatus.CREATED) {

                System.out.println("✅ Salaire de base créé avec succès pour " + baseSalary.getEmployee());
                return true;

            } else {
                System.err.println("❌ Échec création salaire (code " + response.getStatusCode() + ")");
                System.err.println("📋 Réponse: " + response.getBody());
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'insertion du salaire de base: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Vérifier si un employé existe dans ERPNext
     */
    private boolean employeeExists(HttpSession session, String employeeId) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null)
            return false;

        try {
            String url = BASE_URL + "/api/resource/Employee/" + employeeId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            System.err.println("❌ Erreur vérification employé " + employeeId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupérer la structure salariale par défaut pour une entreprise
     */
    private String getDefaultSalaryStructure(HttpSession session, String company) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null)
            return null;

        try {
            // Filtre pour structures actives de l'entreprise
            String filtersJson = "[[\"Salary Structure\",\"company\",\"=\",\"" + company + "\"]," +
                    "[\"Salary Structure\",\"docstatus\",\"=\",\"1\"]]";

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + "/api/resource/Salary Structure")
                    .queryParam("fields", "[\"name\",\"is_default\"]")
                    .queryParam("filters", filtersJson)
                    .queryParam("order_by", "is_default desc, creation desc")
                    .queryParam("limit_page_length", "1")
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
                };
                Map<String, Object> result = objectMapper.readValue(response.getBody(), typeRef);

                if (result.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> structures = (List<Map<String, Object>>) result.get("data");

                    if (!structures.isEmpty()) {
                        Map<String, Object> structure = structures.get(0);
                        String structureName = (String) structure.get("name");
                        System.out.println("🏗️ Structure salariale trouvée: " + structureName);
                        return structureName;
                    }
                }
            }

            // Si aucune structure trouvée, utiliser une valeur par défaut
            System.out.println("⚠️ Aucune structure trouvée pour " + company + ", utilisation de STRUCT1");
            return "STRUCT1";

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération structure salariale: " + e.getMessage());
            return "STRUCT1"; // Valeur par défaut
        }
    }

    public boolean insertBaseSalariesBatch(List<BaseSalary> baseSalaries, HttpSession session) {
        boolean allSuccess = true;
        int successCount = 0;
        int failCount = 0;

        System.out.println("🚀 Début insertion en lot de " + baseSalaries.size() + " salaires de base");

        for (BaseSalary baseSalary : baseSalaries) {
            try {
                boolean success = insertBaseSalary(baseSalary, session);

                if (success) {
                    successCount++;
                    System.out.println("✅ Succès pour " + baseSalary.getEmployee());
                } else {
                    failCount++;
                    allSuccess = false;
                    System.err.println("❌ Échec pour " + baseSalary.getEmployee());
                }

                // Pause entre les insertions pour éviter la surcharge
                Thread.sleep(500);

            } catch (Exception e) {
                failCount++;
                allSuccess = false;
                System.err.println("❌ Erreur pour " + baseSalary.getEmployee() + ": " + e.getMessage());
            }
        }

        System.out.println("📊 Résultat insertion lot: " + successCount + " succès, " + failCount + " échecs");
        return allSuccess;
    }

    /**
     * Vérifier si un employé a déjà un salaire de base actif
     */
    public boolean hasActiveSalaryAssignment(HttpSession session, String employeeId) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null)
            return false;

        try {
            String filtersJson = "[[\"Salary Structure Assignment\",\"employee\",\"=\",\"" + employeeId + "\"]," +
                    "[\"Salary Structure Assignment\",\"docstatus\",\"=\",\"1\"]]";

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + "/api/resource/Salary Structure Assignment")
                    .queryParam("fields", "[\"name\",\"from_date\"]")
                    .queryParam("filters", filtersJson)
                    .queryParam("order_by", "from_date desc")
                    .queryParam("limit_page_length", "1")
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
                };
                Map<String, Object> result = objectMapper.readValue(response.getBody(), typeRef);

                if (result.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> assignments = (List<Map<String, Object>>) result.get("data");

                    boolean hasActive = !assignments.isEmpty();
                    System.out.println("🔍 Employé " + employeeId + " a un assignment actif: " + hasActive);
                    return hasActive;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur vérification assignment actif: " + e.getMessage());
        }

        return false;
    }

    public boolean genererSalaire(
            String employeRef,
            String dateDebut,
            String dateFin,
            String company,
            String salaryStructure,
            double montant,
            HttpSession session) {

        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            System.err.println("❌ Session non authentifiée");
            return false;
        }

        try {
            if (!employeeExists(session, employeRef)) {
                System.err.println("❌ Employé inexistant : " + employeRef);
                return false;
            }

            if (salaryStructure == null || salaryStructure.isEmpty()) {
                System.err.println("❌ Structure salariale non spécifiée");
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", "sid=" + sid);
            headers.set("Expect", "");

            if (montant == 0.0) {
                montant = recupererDernierSalaireBase(employeRef, headers);
                if (montant == 0.0) {
                    System.err.println("❌ Salaire de base introuvable pour " + employeRef);
                    return false;
                }
            }

            LocalDate start = LocalDate.parse(dateDebut).withDayOfMonth(1);
            LocalDate end = LocalDate.parse(dateFin).withDayOfMonth(1);

            while (!start.isAfter(end)) {
                LocalDate moisDebut = start.withDayOfMonth(1);
                LocalDate moisFin = start.withDayOfMonth(start.lengthOfMonth());

                // Vérifier existence du Salary Structure Assignment
                if (!existsSalaryStructureAssignment(employeRef, moisDebut, headers)) {
                    Map<String, Object> assignmentPayload = new HashMap<>();
                    assignmentPayload.put("employee", employeRef);
                    assignmentPayload.put("company", company);
                    assignmentPayload.put("from_date", moisDebut.toString());
                    assignmentPayload.put("salary_structure", salaryStructure);
                    assignmentPayload.put("base", montant);
                    assignmentPayload.put("docstatus", 1);

                    String assignmentJson = objectMapper.writeValueAsString(assignmentPayload);
                    HttpEntity<String> assignmentEntity = new HttpEntity<>(assignmentJson, headers);

                    ResponseEntity<String> assignmentResponse = restTemplate.postForEntity(
                            BASE_URL + "/api/resource/Salary Structure Assignment",
                            assignmentEntity,
                            String.class);

                    if (!assignmentResponse.getStatusCode().is2xxSuccessful()) {
                        System.err.println("❌ Échec SSA pour " + moisDebut + ": " + assignmentResponse.getBody());
                        return false;
                    }
                } else {
                    System.out.println("⚠️ SSA déjà existant pour " + moisDebut + ", création ignorée.");
                }

                // Vérifier existence du Salary Slip
                if (!existsSalarySlip(employeRef, moisDebut, moisFin, headers)) {
                    Map<String, Object> slipPayload = new HashMap<>();
                    slipPayload.put("employee", employeRef);
                    slipPayload.put("start_date", moisDebut.toString());
                    slipPayload.put("end_date", moisFin.toString());
                    slipPayload.put("salary_structure", salaryStructure);
                    slipPayload.put("company", company);
                    slipPayload.put("docstatus", 1);

                    String slipJson = objectMapper.writeValueAsString(slipPayload);
                    HttpEntity<String> slipEntity = new HttpEntity<>(slipJson, headers);

                    ResponseEntity<String> slipResponse = restTemplate.postForEntity(
                            BASE_URL + "/api/resource/Salary Slip",
                            slipEntity,
                            String.class);

                    if (!slipResponse.getStatusCode().is2xxSuccessful()) {
                        System.err.println("❌ Échec fiche de paie pour " + moisDebut + ": " + slipResponse.getBody());
                        return false;
                    }
                } else {
                    System.out.println("⚠️ Fiche de paie déjà existante pour " + moisDebut + ", création ignorée.");
                }

                System.out.println("✅ Salaire traité pour " + employeRef + " - Mois : " + moisDebut.getMonth());
                start = start.plusMonths(1);
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur génération salaire: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean existsSalaryStructureAssignment(String employeRef, LocalDate moisDebut, HttpHeaders headers) {
        String url = BASE_URL + "/api/resource/Salary Structure Assignment?filters="
                + "[[\"employee\", \"=\", \"" + employeRef + "\"],"
                + "[\"from_date\", \"=\", \"" + moisDebut.toString() + "\"]]";

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                return data != null && data.size() > 0;
            } catch (IOException e) {
                System.err.println("❌ Erreur parsing JSON SSA: " + e.getMessage());
                e.printStackTrace();
                return false; // Ou true selon ta logique, mais false semble plus sûr
            }
        } else {
            System.err.println("❌ Erreur vérification SSA: " + response.getBody());
            return false;
        }
    }

    private boolean existsSalarySlip(String employeRef, LocalDate moisDebut, LocalDate moisFin, HttpHeaders headers) {
        String url = BASE_URL + "/api/resource/Salary Slip?filters="
                + "[[\"employee\", \"=\", \"" + employeRef + "\"],"
                + "[\"start_date\", \"=\", \"" + moisDebut.toString() + "\"],"
                + "[\"end_date\", \"=\", \"" + moisFin.toString() + "\"]]";

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                return data != null && data.size() > 0;
            } catch (IOException e) {
                System.err.println("❌ Erreur parsing JSON Salary Slip: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            System.err.println("❌ Erreur vérification Salary Slip: " + response.getBody());
            return false;
        }
    }

    private double recupererDernierSalaireBase(String employeRef, HttpHeaders headers) {
        try {
            String url = BASE_URL + "/api/resource/Salary Structure Assignment?filters="
                    + "[[\"employee\", \"=\", \"" + employeRef + "\"]]"
                    + "&fields=[\"base\", \"from_date\"]"
                    + "&limit_page_length=1"
                    + "&order_by=from_date desc";

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            System.out.println("Réponse JSON : " + response.getBody()); // DEBUG

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                if (data != null && data.isArray() && data.size() > 0) {
                    JsonNode dernierSSA = data.get(0);
                    System.out.println("Dernier SSA JSON: " + dernierSSA.toString()); // DEBUG
                    return dernierSSA.has("base") ? dernierSSA.get("base").asDouble() : 0.0;
                } else {
                    System.err.println("❌ 'data' est vide ou pas un tableau");
                }
            } else {
                System.err.println("❌ Erreur récupération dernier SSA: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("❌ Exception récupération dernier SSA: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    public List<BaseSalary> getAllBaseSalaries(HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isEmpty()) {
            throw new IllegalArgumentException("SID dans la session est nul ou vide");
        }

        String url = BASE_URL + "/api/resource/Salary Structure?limit_page_length=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<BaseSalaryApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                BaseSalaryApiResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            throw new RuntimeException("Erreur lors de la récupération des salaires de base");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseSalaryApiResponse {
        private List<BaseSalary> data;

        public List<BaseSalary> getData() {
            return data;
        }

        public void setData(List<BaseSalary> data) {
            this.data = data;
        }
    }

}