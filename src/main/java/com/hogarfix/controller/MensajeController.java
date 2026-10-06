package com.hogarfix.controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.model.Mensaje;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;
import com.hogarfix.service.ClienteService;
import com.hogarfix.service.MensajeService;
import com.hogarfix.service.ServicioService;
import com.hogarfix.service.TecnicoService;

import lombok.RequiredArgsConstructor;

/**
 * Chat simple (sin websockets, vía polling) entre el cliente y el técnico
 * de un servicio en concreto.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mensajes")
public class MensajeController {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final MensajeService mensajeService;
    private final ServicioService servicioService;
    private final ClienteService clienteService;
    private final TecnicoService tecnicoService;

    @GetMapping("/{idServicio}")
    public ResponseEntity<?> listarMensajes(@PathVariable Long idServicio, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }

        var servOpt = servicioService.buscarPorId(idServicio);
        if (servOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Servicio servicio = servOpt.get();

        if (!tieneAcceso(servicio, principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes acceso a esta conversación"));
        }

        List<Mensaje> mensajes = mensajeService.listarPorServicio(servicio);
        var resultado = mensajes.stream().map(this::toMap).toList();
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{idServicio}")
    public ResponseEntity<?> enviarMensaje(@PathVariable Long idServicio, @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }

        var servOpt = servicioService.buscarPorId(idServicio);
        if (servOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Servicio servicio = servOpt.get();

        Mensaje.Autor autor = determinarAutor(servicio, principal);
        if (autor == null) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes acceso a esta conversación"));
        }

        String contenido = body.get("contenido");
        try {
            Mensaje mensaje = mensajeService.enviarMensaje(servicio, autor, contenido);
            return ResponseEntity.ok(toMap(mensaje));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private boolean tieneAcceso(Servicio servicio, Principal principal) {
        return determinarAutor(servicio, principal) != null;
    }

    /**
     * Determina si quien hace la petición es el cliente o el técnico de este
     * servicio (o null si no tiene relación con la conversación).
     */
    private Mensaje.Autor determinarAutor(Servicio servicio, Principal principal) {
        String email = principal.getName();

        if (servicio.getCliente() != null) {
            var clienteOpt = clienteService.buscarPorEmail(email);
            if (clienteOpt.isPresent() && servicio.getCliente().getIdCliente() != null
                    && servicio.getCliente().getIdCliente().equals(clienteOpt.get().getIdCliente())) {
                return Mensaje.Autor.CLIENTE;
            }
        }

        if (servicio.getTecnico() != null) {
            try {
                Tecnico tecnico = tecnicoService.obtenerPorEmail(email);
                if (tecnico != null && servicio.getTecnico().getIdTecnico() != null
                        && servicio.getTecnico().getIdTecnico().equals(tecnico.getIdTecnico())) {
                    return Mensaje.Autor.TECNICO;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Map<String, Object> toMap(Mensaje m) {
        return Map.of(
                "id", m.getIdMensaje(),
                "autor", m.getAutor().name(),
                "contenido", m.getContenido(),
                "fecha", m.getFechaEnvio() != null ? m.getFechaEnvio().format(FORMATO_HORA) : "");
    }
}