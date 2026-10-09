package com.hogarfix.controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.service.NotificacionService;

import lombok.RequiredArgsConstructor;

/** API de la campanita de notificaciones. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notificaciones")
public class NotificacionController {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<?> listar(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        var usuOpt = notificacionService.usuarioPorLogin(principal.getName());
        if (usuOpt.isEmpty()) return ResponseEntity.ok(Map.of("noLeidas", 0, "items", List.of()));
        var usuario = usuOpt.get();

        var items = notificacionService.ultimas(usuario).stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getIdNotificacion());
            m.put("mensaje", n.getMensaje());
            m.put("enlace", n.getEnlace() == null ? "" : n.getEnlace());
            m.put("leida", n.isLeida());
            m.put("fecha", n.getFecha().format(FORMATO));
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "noLeidas", notificacionService.contarNoLeidas(usuario),
                "items", items));
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<?> leer(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        notificacionService.usuarioPorLogin(principal.getName())
                .ifPresent(u -> notificacionService.marcarLeida(id, u));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<?> leerTodas(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        notificacionService.usuarioPorLogin(principal.getName())
                .ifPresent(notificacionService::marcarTodasLeidas);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
