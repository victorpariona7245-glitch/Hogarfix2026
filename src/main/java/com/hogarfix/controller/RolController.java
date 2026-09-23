package com.hogarfix.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.model.Rol;
import com.hogarfix.service.RolService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<Rol>> listar() {
        return ResponseEntity.ok(rolService.listarRoles());
    }

    @PostMapping
    public ResponseEntity<Rol> registrar(@RequestBody Rol rol) {
        return ResponseEntity.ok(rolService.registrarRol(rol));
    }
}