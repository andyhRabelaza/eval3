package com.spring.erpnext.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;

import jakarta.servlet.http.HttpSession;

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
                // System.out.println("Ligne lue : " + line); // Affichage dans le terminal
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

            ObjectMapper mapper = new ObjectMapper();

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isFirstLine) {
                    isFirstLine = false; // on ignore l’en-tête
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length < 7) {
                    System.err.println("Ligne " + lineNumber + " ignorée (format invalide) : " + line);
                    continue;
                }

                String ref = parts[0].trim();
                String lastName = parts[1].trim();
                String firstName = parts[2].trim();
                String gender = parts[3].trim();
                // Exemple de correction simple
                if (gender.equalsIgnoreCase("Feminin") || gender.equalsIgnoreCase("Féminin")) {
                    gender = "Female"; // correspond au genre attendu dans ERPNext
                } else if (gender.equalsIgnoreCase("Masculin") || gender.equalsIgnoreCase("M")) {
                    gender = "Male";
                }
                String dateEmbauche = parts[4].trim();
                String dateNaissance = parts[5].trim();
                String company = parts[6].trim();

                if (ref.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || gender.isEmpty()
                        || dateEmbauche.isEmpty() || dateNaissance.isEmpty() || company.isEmpty()) {
                    System.err.println("Ligne " + lineNumber + " ignorée (champ vide) : " + line);
                    continue;
                }

                String name = String.format("HR-EMP-%05d", Integer.parseInt(ref));

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
            System.err.println("Erreur lors du traitement du fichier : " + e.getMessage());
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
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(inputDate, inputFormatter);
            return outputFormatter.format(date);
        } catch (DateTimeParseException e) {
            System.err.println("Format de date invalide : " + inputDate);
            return ""; // ou tu peux lever une exception selon ton besoin
        }
    }

    public void processFile2(MultipartFile file, HttpSession session) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            String salaryStructureName = null;
            List<Map<String, Object>> earnings = new ArrayList<>();
            List<Map<String, Object>> deductions = new ArrayList<>();

            Map<String, String> composantVersAbbr = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",", -1);

                if (parts.length < 6) {
                    System.err.println("Ligne ignorée (format invalide) : " + line);
                    continue;
                }

                String structure = parts[0].trim();
                String component = parts[1].trim();
                String abbr = parts[2].trim();
                String type = parts[3].trim().toLowerCase();
                String valeur = parts[4].trim(); // ➤ contient déjà la formule
                String remarque = parts[5].trim();

                if (structure.isEmpty() || component.isEmpty() || abbr.isEmpty() || type.isEmpty()
                        || valeur.isEmpty()) {
                    System.err.println("Ligne ignorée (champ vide) : " + line);
                    continue;
                }

                if (salaryStructureName == null) {
                    salaryStructureName = structure;
                }

                composantVersAbbr.put(component.toLowerCase(), abbr);

                // ➤ Nettoyage de la formule (valeur), remplacement des composants par abbr
                String rawFormula = valeur.toLowerCase();
                String cleanedFormula = rawFormula;

                for (Map.Entry<String, String> entry : composantVersAbbr.entrySet()) {
                    cleanedFormula = cleanedFormula.replace(entry.getKey(), entry.getValue());
                }

                // ➤ Remplacer SB (salaire de base) par "base"
                cleanedFormula = cleanedFormula.replaceAll("(?i)sb", "base");

                // ➤ Nettoyage final : supprimer les caractères indésirables
                cleanedFormula = cleanedFormula.replaceAll("[^a-zA-Z0-9_+\\-*/.() ]", "");

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("salary_component", component);
                item.put("abbr", abbr);
                // item.put("amount", 0); // Valeur fixe, ajustable si besoin
                item.put("formula", cleanedFormula);

                if (type.equals("earning")) {
                    earnings.add(item);
                } else if (type.equals("deduction")) {
                    deductions.add(item);
                } else {
                    System.err.println("Type inconnu : " + type);
                }
            }

            Map<String, Object> structureJson = new LinkedHashMap<>();
            structureJson.put("name", salaryStructureName);
            structureJson.put("company", "My Company"); // À adapter si besoin
            structureJson.put("earnings", earnings);
            structureJson.put("deductions", deductions);
            structureJson.put("docstatus", 1);

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(structureJson);

            System.out.println("✅ JSON généré :\n" + jsonData);

            // sendSalaryStructureToAPI(jsonData, session);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement du fichier : " + e.getMessage());
        }
    }

    private void sendSalaryStructureToAPI(String jsonData, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            System.err.println("Erreur : aucune session 'sid' trouvée.");
            return;
        }

        try {
            // Évite le bug 417
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
                    continue; // sauter l'en-tête
                }

                String[] parts = line.split(",", -1);

                if (parts.length < 4) {
                    System.err.println("Ligne ignorée (format invalide) : " + line);
                    continue;
                }

                String mois = parts[0].trim(); // Exemple : 01/04/2025
                String employeeId = parts[1].trim(); // Exemple : 1
                String salaireBaseStr = parts[2].trim(); // Exemple : 1500000
                String structure = parts[3].trim(); // Exemple : gasy1

                if (mois.isEmpty() || employeeId.isEmpty() || salaireBaseStr.isEmpty() || structure.isEmpty()) {
                    System.err.println("Ligne ignorée (champ vide) : " + line);
                    continue;
                }

                double baseSalary = Double.parseDouble(salaireBaseStr);

                // Extraire le mois et l'année
                String[] dateParts = mois.split("/");
                if (dateParts.length != 3) {
                    System.err.println("Format de date invalide : " + mois);
                    continue;
                }

                String start_date = dateParts[2] + "-" + dateParts[1] + "-01"; // ex: 2025-04-01
                String end_date = dateParts[2] + "-" + dateParts[1] + "-30"; // simplification

                Map<String, Object> slip = new LinkedHashMap<>();
                slip.put("employee", String.format("HR-EMP-%05d", Integer.parseInt(employeeId))); // selon votre système
                slip.put("salary_structure", structure);
                slip.put("start_date", start_date);
                slip.put("end_date", end_date);
                slip.put("base", baseSalary);
                slip.put("docstatus", 1);

                slips.add(slip);
            }

            // Regrouper dans un seul JSON si tu veux tout envoyer d’un coup
            Map<String, Object> jsonWrapper = new HashMap<>();
            jsonWrapper.put("salary_slips", slips);

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonWrapper);

            System.out.println("✅ JSON généré :\n" + jsonData);

            // ➤ Tu peux maintenant appeler sendToApi(jsonData)
            sendSalarySlipsToAPI(slips, session);

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
        }
    }

    private void sendSalarySlipsToAPI(List<Map<String, Object>> slips, HttpSession session) {
        String sid = (String) session.getAttribute("sid");

        if (sid == null || sid.isEmpty()) {
            System.err.println("Erreur : aucune session 'sid' trouvée.");
            return;
        }

        for (Map<String, Object> slip : slips) {
            try {
                URL url = new URL("http://erpnext.localhost:8000/api/resource/Salary Slip");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Cookie", "sid=" + sid);
                conn.setDoOutput(true);

                // Convertir la map slip en JSON
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("employee", slip.get("employee"));
                wrapper.put("salary_structure", slip.get("salary_structure"));
                wrapper.put("start_date", slip.get("start_date"));
                wrapper.put("end_date", slip.get("end_date"));
                wrapper.put("base", slip.get("base"));
                wrapper.put("docstatus", slip.get("docstatus"));

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(wrapper);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    System.out.println("✅ Slip envoyé : " + slip.get("employee"));
                } else {
                    System.err.println("❌ Échec (code " + responseCode + ") pour " + slip.get("employee"));
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

}
