package com.hogarfix.controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.model.Cliente;
import com.hogarfix.model.Tecnico;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.CalificacionRepository;
import com.hogarfix.repository.TecnicoRepository;
import com.hogarfix.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reseñas de los técnicos (solo lectura). Requiere haber iniciado sesión.
 * El cliente aparece abreviado (nombre + inicial) para cuidar su privacidad.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/resenas")
public class ResenaController {

    private static final int MAX_RESENAS = 20;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CalificacionRepository calificacionRepository;
    private final TecnicoRepository tecnicoRepository;
    private final UsuarioRepository usuarioRepository;

    public record ResenaDTO(int puntuacion, String comentario, String fecha, String cliente) {
    }

    public record ResumenDTO(double promedio, long total, List<ResenaDTO> resenas) {
    }

    @GetMapping("/tecnico/{id}")
    public ResponseEntity<ResumenDTO> deTecnico(@PathVariable Long id) {
        return tecnicoRepository.findById(id)
                .map(t -> ResponseEntity.ok(armar(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Reseñas del técnico que tiene la sesión iniciada. */
    @GetMapping("/mias")
    public ResponseEntity<ResumenDTO> mias(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String nombre = principal.getName();
        Tecnico tecnico = usuarioRepository.findByEmail(nombre)
                .or(() -> usuarioRepository.findByUsername(nombre))
                .map(Usuario::getTecnico)
                .orElse(null);
        if (tecnico == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(armar(tecnico));
    }

    private ResumenDTO armar(Tecnico tecnico) {
        Long id = tecnico.getIdTecnico();
        double promedio = tecnico.getPromedioCalificacion() != null ? tecnico.getPromedioCalificacion() : 0.0;
        long total = calificacionRepository.contarDeTecnico(id);
        List<ResenaDTO> resenas = calificacionRepository
                .findResenasDeTecnico(id, PageRequest.of(0, MAX_RESENAS)).stream()
                .map(c -> new ResenaDTO(
                        c.getPuntuacion(),
                        c.getObservacion() == null ? "" : c.getObservacion().trim(),
                        c.getFechaCalificacion() == null ? "" : c.getFechaCalificacion().format(FECHA),
                        nombreCorto(c.getServicio().getCliente())))
                .toList();
        return new ResumenDTO(promedio, total, resenas);
    }

    private static String nombreCorto(Cliente cliente) {
        if (cliente == null || cliente.getNombres() == null || cliente.getNombres().isBlank()) {
            return "Cliente";
        }
        String primero = cliente.getNombres().trim().split("\\s+")[0];
        String apellido = cliente.getApellidoPaterno();
        if (apellido != null && !apellido.isBlank()) {
            return primero + " " + apellido.trim().substring(0, 1).toUpperCase() + ".";
        }
        return primero;
    }
}
