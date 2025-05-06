package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class MinesweeperApplication {

    public static void main(String[] args) {
        // Leggi la variabile d'ambiente FRONT direttamente dal sistema
        String frontUrl = System.getenv("FRONT");

        // Imposta la variabile come proprietà di sistema per usarla in Spring Boot
        System.setProperty("FRONT", frontUrl);

        // Avvia l'applicazione Spring Boot
        SpringApplication.run(MinesweeperApplication.class, args);
    }

    @CrossOrigin(origins = "${FRONT}", allowCredentials = "true")
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }
}
