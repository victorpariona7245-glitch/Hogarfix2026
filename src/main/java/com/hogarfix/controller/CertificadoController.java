package com.hogarfix.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hogarfix.config.SecurityUtils;
import com.hogarfix.model.Tecnico;
import com.hogarfix.service.TecnicoService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/tecnicos")
public class CertificadoController {

    // Carpeta donde se guardan los certificados (misma que usa TecnicoController al subirlos)
    private static final Path DIRECTORIO = Paths.get("uploads", "certificados").toAbsolutePath().normalize();

    private final TecnicoService tecnicoService;

    @GetMapping("/certificados/{nombre}")
    public ResponseEntity<Resource> verCertificado(@PathVariable String nombre) throws IOException {

        if (nombre == null || nombre.contains("..") || nombre.contains("/") || nombre.contains("\\")) {
            return ResponseEntity.notFound().build();
        }

        // Los certificados tienen datos personales: solo los ve el admin o el técnico dueño.
        if (!puedeVer(nombre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path rutaArchivo = DIRECTORIO.resolve(nombre).normalize();
        if (!rutaArchivo.startsWith(DIRECTORIO)) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new UrlResource(rutaArchivo.toUri());
        if (!recurso.exists() || !recurso.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificado.pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso);
    }

    private boolean puedeVer(String nombre) {
        if (SecurityUtils.esAdmin()) {
            return true;
        }
        var auth = SecurityUtils.autenticacion();
        if (auth == null) {
            return false;
        }
        try {
            Tecnico tecnico = tecnicoService.obtenerPorEmail(auth.getName());
            String ruta = tecnico.getCertificadoPdf();
            return ruta != null && (ruta.equals(nombre) || ruta.endsWith("/" + nombre));
        } catch (RuntimeException ex) {
            // no es técnico (por ejemplo un cliente): no puede ver certificados
            return false;
        }
    }
}
