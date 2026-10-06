package com.hogarfix.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.hogarfix.model.Pago;
import com.hogarfix.model.Usuario;

/**
 * Utilidades para saber quién es el usuario autenticado y si puede tocar un recurso.
 * Sirve para validar que un cliente solo opere sobre SUS propios pagos, etc.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Devuelve la autenticación actual, o null si no hay un usuario real logueado. */
    public static Authentication autenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth;
    }

    public static boolean esAdmin() {
        Authentication auth = autenticacion();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Id del usuario autenticado, o null si no se puede determinar. */
    public static Long idUsuarioActual() {
        Authentication auth = autenticacion();
        if (auth != null && auth.getPrincipal() instanceof Usuario) {
            return ((Usuario) auth.getPrincipal()).getIdUsuario();
        }
        return null;
    }

    /** Un pago solo puede ser gestionado por el cliente dueño del pago o por un administrador. */
    public static boolean puedeAccederAPago(Pago pago) {
        if (esAdmin()) {
            return true;
        }
        Long idActual = idUsuarioActual();
        if (idActual == null || pago == null || pago.getCliente() == null
                || pago.getCliente().getUsuario() == null) {
            return false;
        }
        return idActual.equals(pago.getCliente().getUsuario().getIdUsuario());
    }
}
