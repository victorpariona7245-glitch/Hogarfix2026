package com.hogarfix.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hogarfix.model.Ciudad;
import com.hogarfix.service.CiudadService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ciudades")
public class CiudadController {

    private final CiudadService ciudadService;

    @GetMapping
    public ResponseEntity<List<Ciudad>> listar() {
        return ResponseEntity.ok(ciudadService.listarCiudades());
    }

    @PostMapping
    public ResponseEntity<Ciudad> registrar(@RequestBody Ciudad ciudad) {
        return ResponseEntity.ok(ciudadService.registrarCiudad(ciudad));
    }
}