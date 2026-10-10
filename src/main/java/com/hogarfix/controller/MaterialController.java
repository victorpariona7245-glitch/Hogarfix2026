package com.hogarfix.controller;

import java.security.Principal;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hogarfix.config.SecurityUtils;
import com.hogarfix.service.MaterialService;

import lombok.RequiredArgsConstructor;

/** Muestra el comprobante de un material: solo el cliente, el técnico del servicio y los administradores. */
@Controller
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping("/materiales/{id}/comprobante")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> comprobante(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var opt = materialService.buscar(id);
        if (opt.isEmpty() || opt.get().getComprobante() == null) return ResponseEntity.notFound().build();
        var s = opt.get().getServicio();
        String login = principal.getName();
        boolean esCliente = s.getCliente() != null && s.getCliente().getUsuario() != null
                && login.equalsIgnoreCase(s.getCliente().getUsuario().getEmail());
        boolean esTecnico = s.getTecnico() != null && s.getTecnico().getUsuario() != null
                && login.equalsIgnoreCase(s.getTecnico().getUsuario().getEmail());
        if (!esCliente && !esTecnico && !SecurityUtils.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(opt.get().getComprobante());
    }
}
