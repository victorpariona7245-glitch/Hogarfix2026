package com.hogarfix.config;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hogarfix.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Si el administrador suspende una cuenta mientras esa persona tiene la sesión abierta,
 * en su siguiente petición se le cierra la sesión y se le envía al inicio de sesión.
 */
@Component
@RequiredArgsConstructor
public class SuspensionFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/images/")
                || ruta.startsWith("/auth/") || ruta.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String nombre = auth.getName();
            boolean suspendido = usuarioRepository.existsByEmailAndSuspendidoTrue(nombre)
                    || usuarioRepository.existsByUsernameAndSuspendidoTrue(nombre);
            if (suspendido) {
                HttpSession sesion = request.getSession(false);
                if (sesion != null) {
                    sesion.invalidate();
                }
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/auth/login?error=suspendida");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
