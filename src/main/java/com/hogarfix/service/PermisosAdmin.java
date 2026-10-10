package com.hogarfix.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hogarfix.model.Usuario;

/** Qué secciones del panel puede ver cada administrador. El administrador principal puede todo. */
@Component
public class PermisosAdmin {

    public static final String TODOS = "*";

    /** clave → nombre que se muestra. "Resumen" (dashboard) siempre está disponible. */
    public static final Map<String, String> SECCIONES = new LinkedHashMap<>();
    static {
        SECCIONES.put("reportes", "Reporte mensual");
        SECCIONES.put("clientes", "Clientes");
        SECCIONES.put("tecnicos", "Técnicos");
        SECCIONES.put("categorias", "Especialidades");
        SECCIONES.put("actividad", "Actividad");
        SECCIONES.put("pagos", "Pagos");
        SECCIONES.put("comisiones", "Cobros y comisiones");
        SECCIONES.put("disputas", "Disputas");
        SECCIONES.put("certificados", "Certificados");
    }

    @Value("${app.admin.email:}")
    private String adminEmailConfig;

    public String emailPrincipal() {
        return (adminEmailConfig == null || adminEmailConfig.isBlank()) ? "admin@example.com" : adminEmailConfig.trim();
    }

    public boolean esPrincipal(Usuario u) {
        return u != null && u.getEmail() != null && u.getEmail().equalsIgnoreCase(emailPrincipal());
    }

    /** Claves permitidas; {"*"} significa todas. */
    public Set<String> permisosDe(Usuario u) {
        if (esPrincipal(u) || u == null || u.getPermisos() == null) return Set.of(TODOS);
        return Arrays.stream(u.getPermisos().split(","))
                .map(String::trim)
                .filter(SECCIONES::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Convierte lo marcado en el formulario en el texto que se guarda (solo claves válidas). */
    public String normalizar(List<String> marcados) {
        if (marcados == null) return "";
        return marcados.stream().map(String::trim).filter(SECCIONES::containsKey).distinct()
                .collect(Collectors.joining(","));
    }
}
