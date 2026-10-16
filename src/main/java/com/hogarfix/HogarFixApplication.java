package com.hogarfix;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class HogarFixApplication {

	// Railway corre en hora UTC: se fija la hora de Perú para que fechas, chat y boletas salgan bien
	@PostConstruct
	void zonaHoraria() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
		SpringApplication.run(HogarFixApplication.class, args);
	}
}
