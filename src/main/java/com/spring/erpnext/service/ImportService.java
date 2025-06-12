package com.spring.erpnext.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;

import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ImportService {

    // Méthode simple : lire brut les lignes
    public List<String> readFileLines(MultipartFile file) {
        List<String> lignes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lignes.add(line);
                System.out.println("Ligne lue : " + line); // Affichage dans le terminal
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }

        return lignes;
    }

    public void processFile1(MultipartFile file, HttpSession session) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isFirstLine) {
                    isFirstLine = false; // on ignore l’en-tête
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length < 7) {
                    System.err
                            .println("❌ Erreur à la ligne " + lineNumber + " : format invalide (moins de 7 colonnes).");
                    return;
                }

                String ref = parts[0].trim();
                String lastName = parts[1].trim();
                String firstName = parts[2].trim();
                String gender = parts[3].trim();
                String dateEmbauche = parts[4].trim();
                String dateNaissance = parts[5].trim();
                String company = parts[6].trim();

                // Vérification des champs vides
                if (ref.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || gender.isEmpty()
                        || dateEmbauche.isEmpty() || dateNaissance.isEmpty() || company.isEmpty()) {
                    System.err.println("❌ Erreur à la ligne " + lineNumber + " : un ou plusieurs champs sont vides.");
                    return;
                }

                // Normalisation du genre
                if (gender.equalsIgnoreCase("Feminin") || gender.equalsIgnoreCase("Féminin")) {
                    gender = "Female";
                } else if (gender.equalsIgnoreCase("Masculin") || gender.equalsIgnoreCase("M")) {
                    gender = "Male";
                } else if (!gender.equalsIgnoreCase("Male") && !gender.equalsIgnoreCase("Female")) {
                    System.err.println("❌ Erreur à la ligne " + lineNumber + " : genre invalide (« " + gender + " »).");
                    return;
                }

                // Construction du nom unique de l'employé
                String name;
                try {
                    name = String.format("HR-EMP-%05d", Integer.parseInt(ref));
                } catch (NumberFormatException e) {
                    System.err
                            .println("❌ Erreur à la ligne " + lineNumber + " : référence invalide (« " + ref + " »).");
                    return;
                }

                Map<String, String> jsonMap = new LinkedHashMap<>();
                jsonMap.put("name", name);
                jsonMap.put("first_name", firstName);
                jsonMap.put("last_name", lastName);
                jsonMap.put("gender", gender);
                jsonMap.put("date_of_joining", formatDate(dateEmbauche));
                jsonMap.put("date_of_birth", formatDate(dateNaissance));
                jsonMap.put("company", company);

                ObjectMapper jsonMapper = new ObjectMapper();
                String jsonData = jsonMapper.writeValueAsString(jsonMap);

                sendToAPI(jsonData, session);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement du fichier : " + e.getMessage());
        }
    }

    private void sendToAPI(String jsonData, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            System.err.println("Erreur : aucune session 'sid' trouvée.");
            return;
        }

        try {
            URL url = new URL("http://erpnext.localhost:8000/api/resource/Employee");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Cookie", "sid=" + sid);
            conn.setRequestProperty("Expect", ""); // <<< Ajouté ici pour éviter le 417
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("Employé importé avec succès.");
            } else {
                System.err.println("Échec de l'importation : Code " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi à l'API : " + e.getMessage());
        }
    }

    private String formatDate(String inputDate) {
        String[] possibleFormats = {
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd" // au cas où la date est déjà bien formatée
        };

        for (String format : possibleFormats) {
            try {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(format);
                LocalDate date = LocalDate.parse(inputDate, inputFormatter);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException ignored) {
                // On essaie le format suivant
            }
        }

        System.err.println("❌ Format de date invalide : " + inputDate);
        return ""; // ou lever une exception si nécessaire
    }

    public void processFile2(MultipartFile file, HttpSession session) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            Map<String, List<String[]>> groupedLines = new LinkedHashMap<>();

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 6) {
                    System.err.println("❌ Ligne " + lineNumber + " ignorée (colonnes manquantes) : " + line);
                    continue;
                }

                String structure = parts[0].trim();
                String component = parts[1].trim();
                String abbr = parts[2].trim();
                String typeRaw = parts[3].trim();
                String valeur = parts[4].trim();

                if (structure.isEmpty() || component.isEmpty() || abbr.isEmpty()
                        || typeRaw.isEmpty() || valeur.isEmpty()) {
                    System.err.println("❌ Ligne " + lineNumber + " ignorée (champs vides) : " + line);
                    continue;
                }

                groupedLines.computeIfAbsent(structure, k -> new ArrayList<>()).add(parts);
            }

            java.util.function.Function<String, String> normalizeType = (rawType) -> {
                if (rawType.equalsIgnoreCase("earning")) {
                    return "Earning";
                } else if (rawType.equalsIgnoreCase("deduction")) {
                    return "Deduction";
                } else {
                    return rawType;
                }
            };

            for (Map.Entry<String, List<String[]>> entry : groupedLines.entrySet()) {
                String salaryStructureName = entry.getKey();
                List<String[]> lines = entry.getValue();

                // Construire map componentName (en minuscule) -> abbr
                Map<String, String> composantVersAbbr = new HashMap<>();
                for (String[] parts : lines) {
                    String component = parts[1].trim();
                    String abbr = parts[2].trim();
                    composantVersAbbr.put(component.toLowerCase(), abbr);
                }

                List<Map<String, Object>> earnings = new ArrayList<>();
                List<Map<String, Object>> deductions = new ArrayList<>();

                for (String[] parts : lines) {
                    String component = parts[1].trim();
                    String abbr = parts[2].trim();
                    String type = normalizeType.apply(parts[3].trim());
                    String valeur = parts[4].trim();

                    processComponent(component, abbr, type, valeur, session);

                    // Remplacer les noms de composants dans la formule par leurs abbréviations
                    String cleanedFormula = valeur;
                    for (Map.Entry<String, String> e : composantVersAbbr.entrySet()) {
                        cleanedFormula = cleanedFormula.replaceAll("(?i)\\b" + Pattern.quote(e.getKey()) + "\\b",
                                e.getValue());
                        cleanedFormula = cleanedFormula.replaceAll("(?i)\\b" + Pattern.quote(e.getValue()) + "\\b",
                                e.getValue());
                    }

                    cleanedFormula = cleanedFormula.replaceAll("(?i)\\bSB\\b", "base");
                    cleanedFormula = cleanedFormula.replaceAll("[^a-zA-Z0-9_+\\-*/.() ]", "");

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("salary_component", component);
                    item.put("abbr", abbr);
                    item.put("formula", cleanedFormula);
                    item.put("amount_based_on_formula", true);

                    if ("Earning".equals(type)) {
                        earnings.add(item);
                    } else if ("Deduction".equals(type)) {
                        deductions.add(item);
                    } else {
                        System.err.println("❌ Ligne ignorée (type inconnu) : " + type);
                    }
                }

                Map<String, Object> structureJson = new LinkedHashMap<>();
                structureJson.put("name", salaryStructureName);
                structureJson.put("company", "My Company");
                structureJson.put("earnings", earnings);
                structureJson.put("deductions", deductions);
                structureJson.put("docstatus", 1);

                ObjectMapper mapper = new ObjectMapper();
                String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(structureJson);

                System.out.println("✅ JSON généré pour [" + salaryStructureName + "] :\n" + jsonData);

                sendSalaryStructureToAPI(jsonData, session);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement du fichier : " + e.getMessage());
        }
    }

    private void processComponent(String name, String abbr, String type, String formula, HttpSession session) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String sid = (String) session.getAttribute("sid");
            if (sid != null) {
                headers.add("Cookie", "sid=" + sid);
            } else {
                System.err.println("⚠️ SID introuvable dans la session.");
                return;
            }

            String encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20");

            String checkUrl = "http://erpnext.localhost:8000/api/resource/Salary Component/" + encodedName;
            HttpEntity<String> checkEntity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> checkResponse = restTemplate.exchange(
                        checkUrl,
                        HttpMethod.GET,
                        checkEntity,
                        String.class);

                if (checkResponse.getStatusCode().is2xxSuccessful()) {
                    System.out.println("ℹ️ Le composant '" + name + "' existe déjà. Pas de création.");
                    return;
                }
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("🔍 Le composant '" + name + "' n'existe pas encore. Création en cours...");
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la vérification de l'existence du composant '" + name + "' : "
                        + e.getMessage());
                return;
            }

            Map<String, Object> componentData = new LinkedHashMap<>();
            componentData.put("salary_component", name);
            componentData.put("abbr", abbr);
            componentData.put("type", type);
            componentData.put("formula", formula);
            componentData.put("amount_based_on_formula", 1);
            componentData.put("depends_on_payment_days", 0);

            HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(componentData, headers);

            ResponseEntity<String> createResponse = restTemplate.postForEntity(
                    "http://erpnext.localhost:8000/api/resource/Salary Component",
                    createEntity,
                    String.class);

            if (createResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Composant '" + name + "' créé avec succès !");
            } else {
                System.err.println("❌ Échec de création du composant '" + name + "' : " + createResponse.getBody());
            }

        } catch (Exception e) {
            System.err.println("❌ Exception lors du traitement du composant '" + name + "' : " + e.getMessage());
        }
    }

    private void sendSalaryStructureToAPI(String jsonData, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            System.err.println("Erreur : aucune session 'sid' trouvée.");
            return;
        }

        try {
            System.setProperty("sun.net.http.retryPost", "false");

            URL url = new URL("http://erpnext.localhost:8000/api/resource/Salary%20Structure");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Cookie", "sid=" + sid);
            conn.setRequestProperty("Expect", "");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("✅ Salary Structure importée avec succès.");
            } else {
                System.err.println("❌ Échec de l'importation : Code " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.err.println(line);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi à l'API : " + e.getMessage());
        }
    }

    public void processFile3(MultipartFile file, HttpSession session) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            List<Map<String, Object>> slips = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Ignorer l'en-tête
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 4) {
                    System.err.println("❌ Ligne ignorée (format invalide) : " + line);
                    continue;
                }

                String mois = parts[0].trim();
                String employeeIdStr = parts[1].trim();
                String salaireBaseStr = parts[2].trim();
                String structure = parts[3].trim();

                if (mois.isEmpty() || employeeIdStr.isEmpty() || salaireBaseStr.isEmpty() || structure.isEmpty()) {
                    System.err.println("❌ Ligne ignorée (champ vide) : " + line);
                    continue;
                }

                int employeeId;
                double baseSalary;
                try {
                    employeeId = Integer.parseInt(employeeIdStr);
                    baseSalary = Double.parseDouble(salaireBaseStr);
                } catch (NumberFormatException e) {
                    System.err.println("❌ Format invalide (employeeId ou salaire) : " + line);
                    continue;
                }

                // ✅ Traitement de la date avec format flexible
                LocalDate dateParsed = parseFlexibleDate(mois);
                if (dateParsed == null) {
                    System.err.println("❌ Format de date invalide : " + mois);
                    continue;
                }

                LocalDate startDate = dateParsed.withDayOfMonth(1);
                LocalDate endDate = dateParsed.withDayOfMonth(dateParsed.lengthOfMonth());

                Map<String, Object> slip = new LinkedHashMap<>();
                slip.put("employee", String.format("HR-EMP-%05d", employeeId));
                slip.put("salary_structure", structure);
                slip.put("start_date", startDate.toString()); // yyyy-MM-dd
                slip.put("end_date", endDate.toString()); // yyyy-MM-dd
                slip.put("base", baseSalary);
                slip.put("docstatus", 1);

                assignSalaryStructureToEmployee(slip, session);

                slips.add(slip);
            }

            Map<String, Object> jsonWrapper = new HashMap<>();
            jsonWrapper.put("salary_slips", slips);

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonWrapper);

            System.out.println("✅ JSON généré :\n" + jsonData);

            sendSalarySlipsToAPI(slips, session);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LocalDate parseFlexibleDate(String inputDate) {
        String[] formats = { "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd" };

        for (String pattern : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(inputDate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null; // Aucun format valide
    }

    private void sendSalarySlipsToAPI(List<Map<String, Object>> slips, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            System.err.println("Erreur : aucune session 'sid' trouvée.");
            return;
        }

        for (Map<String, Object> slip : slips) {
            try {
                URL url = new URL("http://erpnext.localhost:8000/api/resource/Salary%20Slip");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Cookie", "sid=" + sid);
                conn.setDoOutput(true);

                String employeeId = (String) slip.get("employee"); // ex: "HR-EMP-00011"
                // Construire naming_series pour laisser ERPNext générer le suffixe automatique
                String namingSeries = "Sal Slip/" + employeeId + "/";

                // Construire la charge utile sans 'base'
                Map<String, Object> payload = new HashMap<>();
                payload.put("employee", employeeId);
                payload.put("salary_structure", slip.get("salary_structure"));
                payload.put("start_date", slip.get("start_date"));
                payload.put("end_date", slip.get("end_date"));
                payload.put("docstatus", slip.get("docstatus"));
                payload.put("naming_series", namingSeries);

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(payload);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    System.out.println("✅ Slip envoyé : " + employeeId);
                } else {
                    System.err.println("❌ Échec (code " + responseCode + ") pour " + employeeId);
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.err.println(line);
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur API : " + e.getMessage());
            }
        }
    }

    private void assignSalaryStructureToEmployee(Map<String, Object> slip, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            System.err.println("Erreur : aucune session 'sid' trouvée.");
            return;
        }

        try {
            URL url = new URL("http://erpnext.localhost:8000/api/resource/Salary%20Structure%20Assignment");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Cookie", "sid=" + sid);
            conn.setDoOutput(true);

            Map<String, Object> payload = new HashMap<>();
            payload.put("employee", slip.get("employee"));
            payload.put("salary_structure", slip.get("salary_structure"));
            payload.put("from_date", slip.get("start_date"));
            payload.put("base", slip.get("base"));
            payload.put("docstatus", 1); // <-- Ajout ici pour soumettre directement

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(payload);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("✅ Assignation et soumission réussies pour " + slip.get("employee"));
            } else {
                System.err.println("❌ Échec assignation (code " + responseCode + ") pour " + slip.get("employee"));
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur assignation : " + e.getMessage());
        }
    }

}
