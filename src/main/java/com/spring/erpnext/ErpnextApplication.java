package com.spring.erpnext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ErpnextApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpnextApplication.class, args);
	}

}

// package com.spring.erpnext;

// import com.spring.erpnext.service.TestService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
// public class ErpnextApplication implements CommandLineRunner {

// private final TestService testService;

// @Autowired
// public ErpnextApplication(TestService testService) {
// this.testService = testService;
// }

// public static void main(String[] args) {
// SpringApplication.run(ErpnextApplication.class, args);
// }

// @Override
// public void run(String... args) {
// String resultat = testService.testConnection();
// System.out.println("Connexion réussie - Version MariaDB : " + resultat);

// // Optionnel : test du count si la table existe
// try {
// int nb = testService.countEmployees();
// System.out.println("Nombre d'employés : " + nb);
// } catch (Exception e) {
// System.out.println("La table tabEmployee est peut-être absente : " +
// e.getMessage());
// }
// }
// }
