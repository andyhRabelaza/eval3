package com.spring.erpnext.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    public void processFile1(MultipartFile file) {
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
                jsonMap.put("date_embauche", dateEmbauche);
                jsonMap.put("date_naissance", dateNaissance);
                jsonMap.put("company", company);

                // Affichage JSON formaté
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap));
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du fichier : " + e.getMessage());
        }
    }

    public void processFile2(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            String salaryStructureName = null;
            List<Map<String, Object>> earnings = new ArrayList<>();
            List<Map<String, Object>> deductions = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // on saute l'en-tête
                    continue;
                }

                String[] parts = line.split(",", -1); // -1 pour inclure les colonnes vides

                if (parts.length < 6) {
                    System.err.println("Ligne ignorée (format invalide) : " + line);
                    continue;
                }

                String structure = parts[0].trim();
                String component = parts[1].trim();
                String abbr = parts[2].trim();
                String type = parts[3].trim().toLowerCase();
                String valeur = parts[4].trim();
                String remarque = parts[5].trim();

                if (structure.isEmpty() || component.isEmpty() || abbr.isEmpty() || type.isEmpty()
                        || valeur.isEmpty()) {
                    System.err.println("Ligne ignorée (champ vide) : " + line);
                    continue;
                }

                // Mémoriser le nom de la structure
                if (salaryStructureName == null) {
                    salaryStructureName = structure;
                }

                // Transformer pourcentage en décimal
                double pourcentage = 0;
                try {
                    pourcentage = Double.parseDouble(valeur.replace("%", "")) / 100.0;
                } catch (NumberFormatException e) {
                    System.err.println("Pourcentage invalide à la ligne : " + line);
                    continue;
                }

                // Construire la formule
                String formule = remarque.isEmpty()
                        ? String.format("base * %.3f", pourcentage)
                        : String.format("(%s) * %.3f", remarque.toLowerCase(), pourcentage);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("salary_component", component);
                item.put("abbr", abbr);
                item.put("amount", 0);
                item.put("formula", formule);

                if (type.equals("earning")) {
                    earnings.add(item);
                } else if (type.equals("deduction")) {
                    deductions.add(item);
                } else {
                    System.err.println("Type inconnu : " + type);
                }
            }

            // Construction de l'objet JSON final
            Map<String, Object> structureJson = new LinkedHashMap<>();
            structureJson.put("name", salaryStructureName);
            structureJson.put("company", "My Company"); // à adapter selon ton contexte
            structureJson.put("earnings", earnings);
            structureJson.put("deductions", deductions);

            // Afficher le JSON généré
            System.out.println("JSON généré pour Salary Structure :");
            System.out.println(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(structureJson));

        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du fichier : " + e.getMessage());
        }
    }

}
