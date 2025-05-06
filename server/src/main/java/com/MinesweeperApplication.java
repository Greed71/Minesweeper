package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class MinesweeperApplication {

    public static void main(String[] args) {
        // Carica il file .env prima di avviare Spring Boot
        Dotenv dotenv = Dotenv.load();
        
        // Ottieni la variabile FRONT dal file .env
        String frontUrl = dotenv.get("FRONT");

        // Imposta la variabile come proprietà di sistema per usarla in Spring Boot
        System.setProperty("FRONT", frontUrl);

        // Avvia l'applicazione Spring Boot
        SpringApplication.run(MinesweeperApplication.class, args);
    }

    @CrossOrigin(origins = "${FRONT}", allowCredentials = "true")  // Usa il valore di FRONT nella configurazione di CORS
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }
}
