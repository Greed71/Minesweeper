package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class MinesweeperApplication {

	public static void main(String[] args) {
		SpringApplication.run(MinesweeperApplication.class, args);
	}
	
	@CrossOrigin
	@GetMapping("/hello")
	public String hello() {
		return "Hello, World!";
	}

}
