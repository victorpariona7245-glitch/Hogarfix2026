package com.hogarfix.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.model.Pais;
import com.hogarfix.service.PaisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/paises")
public class PaisController {

    private final PaisService paisService;

    @GetMapping
    public ResponseEntity<List<Pais>> listar() {
        return ResponseEntity.ok(paisService.listarPaises());
    }

    @PostMapping
    public ResponseEntity<Pais> registrar(@RequestBody Pais pais) {
        return ResponseEntity.ok(paisService.registrarPais(pais));
    }
}
