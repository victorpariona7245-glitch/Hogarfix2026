package com.hogarfix.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hogarfix.model.Rol;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.RolRepository;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.service.AuditoriaService;

import lombok.RequiredArgsConstructor;

/**
 * Gestión de administradores. Solo el administrador principal
 * (admin@example.com o el definido en ADMIN_EMAIL) puede usarla.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/administradores")
public class AdminUsuariosController {

    private static final String EMAIL_POR_DEFECTO = "admin@example.com";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Value("${app.admin.email:}")
    private String adminEmailConfig;

    private String emailPrincipal() {
        return (adminEmailConfig == null || adminEmailConfig.isBlank())
                ? EMAIL_POR_DEFECTO
                : adminEmailConfig.trim();
    }

    private java.util.Optional<Usuario> usuarioPrincipal(Principal principal) {
        if (principal == null) {
            return java.util.Optional.empty();
        }
        String nombre = principal.getName();
        var usuario = usuarioRepository.findByEmail(nombre);
        if (usuario.isEmpty()) {
            usuario = usuarioRepository.findByUsername(nombre);
        }
        return usuario.filter(u -> u.getEmail().equalsIgnoreCase(emailPrincipal()));
    }

    private boolean esPrincipal(Principal principal) {
        return usuarioPrincipal(principal).isPresent();
    }

    @GetMapping
    public String listar(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        if (!esPrincipal(principal)) {
            ra.addFlashAttribute("mensajeError", "Solo el administrador principal puede gestionar administradores.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("admins", usuarioRepository.findByRol_NombreIgnoreCase("ADMIN"));
        model.addAttribute("emailPrincipal", emailPrincipal());
        return "admin/administradores";
    }

    @PostMapping
    public String crear(@RequestParam String email,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmar,
            Principal principal,
            RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        if (!esPrincipal(principal)) {
            return "redirect:/admin/dashboard";
        }

        String correo = email == null ? "" : email.trim().toLowerCase();
        String usuario = username == null ? "" : username.trim();

        if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || correo.length() > 100) {
            ra.addFlashAttribute("mensajeError", "Ingresa un correo válido.");
        } else if (usuario.length() < 3 || usuario.length() > 50) {
            ra.addFlashAttribute("mensajeError", "El nombre de usuario debe tener entre 3 y 50 caracteres.");
        } else if (password == null || password.length() < 8) {
            ra.addFlashAttribute("mensajeError", "La contraseña debe tener al menos 8 caracteres.");
        } else if (!password.equals(confirmar)) {
            ra.addFlashAttribute("mensajeError", "Las contraseñas no coinciden.");
        } else if (usuarioRepository.existsByEmail(correo)) {
            ra.addFlashAttribute("mensajeError", "Ya existe una cuenta con ese correo.");
        } else if (usuarioRepository.existsByUsername(usuario)) {
            ra.addFlashAttribute("mensajeError", "Ese nombre de usuario ya está en uso.");
        } else {
            Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                    .orElseGet(() -> rolRepository.save(Rol.builder()
                            .nombre("ADMIN")
                            .descripcion("Rol administrador con acceso a dashboard")
                            .build()));

            usuarioRepository.save(Usuario.builder()
                    .username(usuario)
                    .email(correo)
                    .password(passwordEncoder.encode(password))
                    .rol(rolAdmin)
                    .build());
            auditoriaService.registrar(principal.getName(), "Administrador creado",
                    "Creó al administrador " + correo + " (usuario " + usuario + ")");
            ra.addFlashAttribute("mensajeExito", "Administrador " + correo + " creado correctamente.");
        }
        return "redirect:/admin/administradores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
            @RequestParam(defaultValue = "") String passwordPrincipal,
            Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        var principalUsuario = usuarioPrincipal(principal);
        if (principalUsuario.isEmpty()) {
            return "redirect:/admin/dashboard";
        }
        if (!passwordEncoder.matches(passwordPrincipal, principalUsuario.get().getPassword())) {
            auditoriaService.registrar(principal.getName(), "Intento fallido",
                    "Intentó eliminar a un administrador con contraseña incorrecta");
            ra.addFlashAttribute("mensajeError", "Contraseña incorrecta. No se eliminó al administrador.");
            return "redirect:/admin/administradores";
        }

        var admin = usuarioRepository.findByRol_NombreIgnoreCase("ADMIN").stream()
                .filter(u -> u.getIdUsuario().equals(id))
                .findFirst();

        if (admin.isEmpty()) {
            ra.addFlashAttribute("mensajeError", "Ese administrador no existe.");
        } else if (admin.get().getEmail().equalsIgnoreCase(emailPrincipal())) {
            ra.addFlashAttribute("mensajeError", "No puedes eliminar al administrador principal.");
        } else {
            String correoEliminado = admin.get().getEmail();
            usuarioRepository.deleteById(id);
            auditoriaService.registrar(principal.getName(), "Administrador eliminado",
                    "Eliminó al administrador " + correoEliminado);
            ra.addFlashAttribute("mensajeExito", "Administrador eliminado.");
        }
        return "redirect:/admin/administradores";
    }
}
