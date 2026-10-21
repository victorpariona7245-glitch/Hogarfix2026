package com.hogarfix.controller;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.hogarfix.model.Usuario;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.service.AuditoriaService;
import com.hogarfix.service.RespaldoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Descarga de respaldos: solo el administrador principal, confirmando su contraseña. */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/respaldo")
public class RespaldoController {

    private final RespaldoService respaldoService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Value("${app.admin.email:}")
    private String adminEmailConfig;

    private String emailPrincipal() {
        return (adminEmailConfig == null || adminEmailConfig.isBlank()) ? "admin@example.com" : adminEmailConfig.trim();
    }

    private java.util.Optional<Usuario> principalUsuario(Principal p) {
        if (p == null) return java.util.Optional.empty();
        return usuarioRepository.findByEmail(p.getName()).or(() -> usuarioRepository.findByUsername(p.getName()))
                .filter(u -> u.getEmail().equalsIgnoreCase(emailPrincipal()));
    }

    @GetMapping
    public String pagina(Principal principal, RedirectAttributes ra) {
        if (principalUsuario(principal).isEmpty()) {
            ra.addFlashAttribute("mensajeError", "Solo el administrador principal puede descargar respaldos.");
            return "redirect:/admin/dashboard";
        }
        return "admin/respaldo";
    }

    @PostMapping("/descargar")
    public ResponseEntity<StreamingResponseBody> descargar(@RequestParam(defaultValue = "") String password,
            Principal principal) {
        var usuario = principalUsuario(principal);
        if (usuario.isEmpty()) return redireccion("/admin/dashboard");
        if (!passwordEncoder.matches(password, usuario.get().getPassword())) {
            auditoriaService.registrar(principal.getName(), "Intento fallido",
                    "Intentó descargar un respaldo con contraseña incorrecta");
            return redireccion("/admin/respaldo?error=clave");
        }
        String nombre = "hogarfix-respaldo-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".zip";
        auditoriaService.registrar(principal.getName(), "Respaldo descargado", nombre);
        StreamingResponseBody cuerpo = salida -> {
            try {
                respaldoService.escribirZip(salida);
            } catch (SQLException ex) {
                log.error("No se pudo generar el respaldo: {}", ex.getMessage());
                throw new IOException("No se pudo generar el respaldo", ex);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(cuerpo);
    }

    private static ResponseEntity<StreamingResponseBody> redireccion(String destino) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, destino).build();
    }
}
