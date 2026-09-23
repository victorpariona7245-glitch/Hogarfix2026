package com.hogarfix.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tecnicos")
public class CertificadoController {
    // Ruta ABSOLUTA a tu carpeta uploads
    private final String RUTA_CERTIFICADOS = "uploads/certificados/";

    @GetMapping("/certificados/{nombre}")
    public ResponseEntity<Resource> verCertificado(@PathVariable String nombre) throws IOException {

        Path rutaArchivo = Paths.get(RUTA_CERTIFICADOS).resolve(nombre).normalize();
        Resource recurso = new UrlResource(rutaArchivo.toUri());

        if (!recurso.exists() || !recurso.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(recurso);
    }
}
