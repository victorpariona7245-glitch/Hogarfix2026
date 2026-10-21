package com.hogarfix.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hogarfix.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** Avisa en el Resumen del admin si el administrador principal todavía usa una contraseña conocida. */
@ControllerAdvice
@RequiredArgsConstructor
public class AdminAvisosAdvice {

    private static final String[] CLAVES_CONOCIDAS = { "123456789", "123456", "admin", "admin123", "password" };

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmailConfig;

    @ModelAttribute("claveAdminPorDefecto")
    public boolean claveAdminPorDefecto(Principal principal, HttpServletRequest request) {
        if (principal == null || !"/admin/dashboard".equals(request.getRequestURI())) return false;
        String email = (adminEmailConfig == null || adminEmailConfig.isBlank()) ? "admin@example.com" : adminEmailConfig.trim();
        var u = usuarioRepository.findByEmail(principal.getName()).or(() -> usuarioRepository.findByUsername(principal.getName()));
        if (u.isEmpty() || !u.get().getEmail().equalsIgnoreCase(email)) return false;
        for (String c : CLAVES_CONOCIDAS) {
            if (passwordEncoder.matches(c, u.get().getPassword())) return true;
        }
        return false;
    }
}
