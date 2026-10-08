package com.hogarfix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hogarfix.service.PasswordResetService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/olvide")
    public String formularioOlvide() {
        return "auth/olvide-password";
    }

    @PostMapping("/olvide")
    public String enviarEnlace(@RequestParam(defaultValue = "") String email, RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.solicitar(email);
        } catch (Exception ex) {
            // Nunca se revela si el correo existe ni si algo falló
            log.warn("No se pudo procesar la recuperación de contraseña: {}", ex.getMessage());
        }
        redirectAttributes.addFlashAttribute("enviado", true);
        return "redirect:/auth/olvide";
    }

    @GetMapping("/restablecer")
    public String formularioRestablecer(@RequestParam(required = false) String token, Model model) {
        boolean valido = passwordResetService.tokenValido(token);
        model.addAttribute("tokenValido", valido);
        model.addAttribute("token", valido ? token : "");
        return "auth/restablecer-password";
    }

    @PostMapping("/restablecer")
    public String restablecer(@RequestParam(defaultValue = "") String token,
            @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String confirmar,
            Model model) {

        if (!passwordResetService.tokenValido(token)) {
            model.addAttribute("tokenValido", false);
            model.addAttribute("token", "");
            return "auth/restablecer-password";
        }

        String error = null;
        if (password.length() < 8 || password.length() > 100) {
            error = "La contraseña debe tener entre 8 y 100 caracteres.";
        } else if (!password.equals(confirmar)) {
            error = "Las contraseñas no coinciden.";
        }
        if (error != null) {
            model.addAttribute("tokenValido", true);
            model.addAttribute("token", token);
            model.addAttribute("mensajeError", error);
            return "auth/restablecer-password";
        }

        if (!passwordResetService.restablecer(token, password)) {
            model.addAttribute("tokenValido", false);
            model.addAttribute("token", "");
            return "auth/restablecer-password";
        }
        return "redirect:/auth/login?restablecida";
    }
}
