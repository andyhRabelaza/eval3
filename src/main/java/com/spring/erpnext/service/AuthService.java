package com.spring.erpnext.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AuthService {

    public Map<String, String> loginToErpNext(String username, String password) {
        try {
            String url = "http://erpnext.localhost:8000/api/method/login?usr=" + username + "&pwd=" + password;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String sid = null;
            for (String cookie : response.getHeaders().get("Set-Cookie")) {
                if (cookie.startsWith("sid=")) {
                    sid = cookie.split(";")[0].split("=")[1];
                    break;
                }
            }

            if (sid != null) {
                Map<String, String> sessionData = new HashMap<>();
                sessionData.put("sid", sid);
                sessionData.put("username", username);
                sessionData.put("password", password);
                return sessionData;
            }

        } catch (Exception e) {
            e.printStackTrace(); // à éviter en production
        }

        return null; // en cas d’erreur ou sid manquant
    }
}
