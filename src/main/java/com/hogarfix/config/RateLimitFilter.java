package com.hogarfix.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Límite de peticiones por minuto (en memoria) para frenar abusos: intentos masivos de login/registro,
 * spam de mensajes o de reportes. Solo cuenta peticiones que modifican datos (POST/PUT/DELETE).
 * Se reinicia al reiniciar la aplicación.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Ventana(long inicioMs, int cuenta) {}

    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private long ultimaLimpieza = System.currentTimeMillis();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String m = request.getMethod();
        return m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruta = request.getRequestURI();
        String grupo;
        int maximo;
        if (ruta.startsWith("/auth/") || ruta.equals("/clientes/registro") || ruta.equals("/tecnicos/registro")
                || ruta.equals("/tecnicos/login")) {
            grupo = "acceso"; maximo = 20;
        } else if (ruta.startsWith("/api/mensajes")) {
            grupo = "chat"; maximo = 60;
        } else if (ruta.startsWith("/pagos/") || ruta.startsWith("/disputas/")) {
            grupo = "pagos"; maximo = 15;
        } else {
            grupo = "general"; maximo = 120;
        }

        String clave = grupo + "|" + request.getRemoteAddr();
        long ahora = System.currentTimeMillis();
        limpiar(ahora);
        Ventana v = ventanas.merge(clave, new Ventana(ahora, 1),
                (actual, nueva) -> ahora - actual.inicioMs() > 60_000 ? nueva : new Ventana(actual.inicioMs(), actual.cuenta() + 1));

        if (v.cuenta() > maximo) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("Demasiadas solicitudes. Espera un minuto e inténtalo de nuevo.");
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized void limpiar(long ahora) {
        if (ahora - ultimaLimpieza < 120_000) return;
        ultimaLimpieza = ahora;
        ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicioMs() > 120_000);
    }
}
