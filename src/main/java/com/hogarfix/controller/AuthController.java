package com.hogarfix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.hogarfix.config.LoginAttemptService;
import com.hogarfix.model.Usuario;
import com.hogarfix.service.UsuarioService;
import com.hogarfix.repository.ClienteRepository;
import com.hogarfix.repository.TecnicoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;
    private final LoginAttemptService loginAttemptService;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "auth/login";
    }

    // El registro público de usuarios genéricos se eliminó: aceptaba un Usuario completo (incluido su rol)
    // desde el formulario. Los clientes y técnicos se registran en /clientes/registro y /tecnicos/registro,
    // donde el rol se asigna en el servidor.

    @PostMapping("/login")
    public String procesarLogin(@org.springframework.web.bind.annotation.RequestParam("username") String username,
        @org.springframework.web.bind.annotation.RequestParam("password") String password,
        HttpServletRequest request,
        HttpSession session) {
        logger.info("Intento de autenticación: usuario={}", username);

        String claveIntento = LoginAttemptService.clave(username, request.getRemoteAddr());
        if (loginAttemptService.estaBloqueado(claveIntento)) {
            logger.warn("Login bloqueado por demasiados intentos fallidos: usuario={}", username);
            return "redirect:/auth/login?error=bloqueado";
        }

        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(username, password);
            Authentication auth = authenticationManager.authenticate(token);
            loginAttemptService.registrarExito(claveIntento);
            // Cambiar el id de sesión al autenticar evita la fijación de sesión
            request.changeSessionId();
            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.info("Autenticación exitosa: usuario={}", username);
            
            // Persist SecurityContext in HTTP session so Spring Security recognizes the login across requests
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Intentar resolver el usuario por el nombre del principal (puede ser email o username)
            String principalName = auth.getName();
            var usuarioOpt = usuarioService.buscarPorEmail(principalName);
            if (usuarioOpt.isEmpty()) usuarioOpt = usuarioService.buscarPorUsername(principalName);

            if (usuarioOpt.isPresent()) {
                Usuario u = usuarioOpt.get();
                session.setAttribute("usuarioActual", u);
                // establecer un nombre legible según rol
                if (u.getRol() != null && u.getRol().getTipoRol() != null) {
                    switch (u.getRol().getTipoRol()) {
                        case ADMIN:
                            logger.info("Admin autenticado: {}", u.getEmail());
                            session.setAttribute("usuarioNombre", u.getEmail());
                            return "redirect:/admin/dashboard";
                        case TECNICO:
                            tecnicoRepository.findByUsuario_Email(u.getEmail()).ifPresent(t -> {
                                session.setAttribute("usuarioNombre", t.getNombres() + " " + t.getApellidoPaterno());
                                logger.info("Técnico autenticado: {}", u.getEmail());
                            });
                            return "redirect:/tecnicos/panel";
                        case CLIENTE:
                            clienteRepository.findByUsuario_Email(u.getEmail()).ifPresent(c -> {
                                session.setAttribute("usuarioNombre", c.getNombres() + " " + c.getApellidoPaterno());
                                logger.info("Cliente autenticado: {}", u.getEmail());
                            });
                            return "redirect:/"; // home for clients
                        default:
                            session.setAttribute("usuarioNombre", u.getEmail());
                            return "redirect:/";
                    }
                }
            }
            return "redirect:/"; // fallback
        } catch (org.springframework.security.authentication.LockedException ex) {
            logger.warn("Intento de login de una cuenta suspendida: usuario={}", username);
            return "redirect:/auth/login?error=suspendida";
        } catch (AuthenticationException ex) {
            loginAttemptService.registrarFallo(claveIntento);
            logger.warn("Fallo de autenticación: usuario={}, causa={}", username, ex.getMessage());
            return "redirect:/auth/login?error";
        } catch (Exception ex) {
            logger.error("Error inesperado en autenticación: usuario={}, causa={}", username, ex.getMessage());
            return "redirect:/auth/login?error=server";
        }
    }

    /**
     * Este método puede usarse después del login para redirigir al dashboard
     * correcto.
     */
    @GetMapping("/redirect")
    public String redirigirSegunRol(jakarta.servlet.http.HttpSession session, java.security.Principal principal) {
        // Intentamos obtener usuario desde la sesión
        Usuario usuario = (Usuario) session.getAttribute("usuarioActual");

        // Si no está en sesión, intentar cargarlo desde el principal (nombre = email)
        if (usuario == null) {
            // 1) usar el Principal si está disponible
            if (principal != null) {
                var usuarioOpt = usuarioService.buscarPorEmail(principal.getName());
                if (usuarioOpt.isEmpty()) {
                    usuarioOpt = usuarioService.buscarPorUsername(principal.getName());
                }
                if (usuarioOpt.isPresent()) {
                    usuario = usuarioOpt.get();
                }
            }

            // 2) si aún no está, intentar obtener del SecurityContext (por remember-me u otros proveedores)
            if (usuario == null) {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                    Object p = auth.getPrincipal();
                    if (p instanceof Usuario) {
                        usuario = (Usuario) p;
                    } else if (p instanceof org.springframework.security.core.userdetails.UserDetails) {
                        String name = ((org.springframework.security.core.userdetails.UserDetails) p).getUsername();
                        var usuarioOpt2 = usuarioService.buscarPorEmail(name);
                        if (usuarioOpt2.isEmpty()) usuarioOpt2 = usuarioService.buscarPorUsername(name);
                        if (usuarioOpt2.isPresent()) usuario = usuarioOpt2.get();
                    }
                }
            }

            // si lo encontramos, guardarlo en sesión y preparar usuarioNombre
            if (usuario != null) {
                session.setAttribute("usuarioActual", usuario);
                clienteRepository.findByUsuario_Email(usuario.getEmail()).ifPresent(c ->
                        session.setAttribute("usuarioNombre", c.getNombres() + " " + c.getApellidoPaterno())
                );
                tecnicoRepository.findByUsuario_Email(usuario.getEmail()).ifPresent(t ->
                        session.setAttribute("usuarioNombre", t.getNombres() + " " + t.getApellidoPaterno())
                );
            }
        }

        if (usuario == null) {
            // no autenticado o no encontramos usuario -> forzar login
            return "redirect:/auth/login";
        }

        if (usuario.getRol() == null || usuario.getRol().getTipoRol() == null) {
            return "redirect:/auth/login?error=rol_invalido";
        }

        switch (usuario.getRol().getTipoRol()) {
            case ADMIN:
                return "redirect:/admin/dashboard";
            case TECNICO:
                return "redirect:/tecnicos/panel";
            case CLIENTE:
                return "redirect:/clientes/perfil";
            default:
                return "redirect:/auth/login?error=sin_rol";
        }
    }
}