package com.hogarfix.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.model.Direccion;
import com.hogarfix.service.DireccionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/direcciones")
public class DireccionController {

    private final DireccionService direccionService;

    @GetMapping
    public ResponseEntity<List<Direccion>> listar() {
        return ResponseEntity.ok(direccionService.listarDirecciones());
    }

    @PostMapping
    public ResponseEntity<Direccion> registrar(@RequestBody Direccion direccion) {
        return ResponseEntity.ok(direccionService.registrarDireccion(direccion));
    }
}