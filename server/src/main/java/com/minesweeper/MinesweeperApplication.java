package com.minesweeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.minesweeper")
@EntityScan(basePackages = "com.minesweeper")
public class MinesweeperApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinesweeperApplication.class, args);
    }
}
