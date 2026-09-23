package com.hogarfix.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hogarfix.model.Calificacion;
import com.hogarfix.service.CalificacionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calificaciones")
public class CalificacionController {

    private final CalificacionService calificacionService;

    @GetMapping
    public ResponseEntity<List<Calificacion>> listar() {
        return ResponseEntity.ok(calificacionService.listarCalificaciones());
    }

    @PostMapping
    public ResponseEntity<Calificacion> registrar(@RequestBody Calificacion calificacion) {
        return ResponseEntity.ok(calificacionService.registrarCalificacion(calificacion));
    }
}