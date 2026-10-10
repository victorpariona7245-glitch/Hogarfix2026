package com.hogarfix.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.hogarfix.model.Usuario;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.service.PermisosAdmin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Controla a qué secciones de /admin puede entrar cada administrador.
 * También deja en la petición el atributo "permisosAdmin" para que el menú lateral muestre solo lo permitido.
 */
@Component
@RequiredArgsConstructor
public class PermisoAdminInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;
    private final PermisosAdmin permisosAdmin;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (request.getUserPrincipal() == null) return true; // la seguridad ya se encarga
        String nombre = request.getUserPrincipal().getName();
        Usuario u = usuarioRepository.findByEmail(nombre).or(() -> usuarioRepository.findByUsername(nombre)).orElse(null);
        Set<String> permisos = permisosAdmin.permisosDe(u);
        request.setAttribute("permisosAdmin", permisos);
        request.setAttribute("esAdminPrincipal", permisosAdmin.esPrincipal(u));

        String requerido = claveRequerida(request.getRequestURI().substring(request.getContextPath().length()));
        if (requerido == null || permisos.contains(PermisosAdmin.TODOS)) return true;

        if (requerido.equals(PermisosAdmin.TODOS)) { // solo el principal
            response.sendRedirect(request.getContextPath() + "/admin/dashboard?sinpermiso=1");
            return false;
        }
        for (String clave : requerido.split("\\|")) {
            if (permisos.contains(clave)) return true;
        }
        response.sendRedirect(request.getContextPath() + "/admin/dashboard?sinpermiso=1");
        return false;
    }

    /** Qué permiso pide una ruta (varias opciones separadas por |). null = libre para cualquier admin. */
    static String claveRequerida(String ruta) {
        if (!ruta.startsWith("/admin/")) return null;
        String resto = ruta.substring("/admin/".length());
        String seg = resto.contains("/") ? resto.substring(0, resto.indexOf('/')) : resto;
        boolean habilitar = ruta.contains("/habilitar") || ruta.contains("/deshabilitar");
        return switch (seg) {
            case "reportes" -> "reportes";
            case "exportar" -> "reportes|pagos";
            case "clientes", "categorias", "actividad", "pagos", "comisiones", "disputas" -> seg;
            case "tecnicos", "certificados" -> habilitar ? "tecnicos|certificados" : seg;
            case "administradores" -> PermisosAdmin.TODOS;
            default -> null; // dashboard, etc.
        };
    }
}
