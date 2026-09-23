package com.hogarfix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HogarFixApplication {

	public static void main(String[] args) {
		SpringApplication.run(HogarFixApplication.class, args);
	}
}