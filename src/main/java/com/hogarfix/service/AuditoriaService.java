package com.hogarfix.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.hogarfix.model.RegistroActividad;
import com.hogarfix.repository.RegistroActividadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Guarda quién hizo qué en el panel de administración.
 * Nunca interrumpe la acción original: si no se puede guardar, solo avisa en el log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final RegistroActividadRepository repository;

    public void registrar(String administrador, String accion, String detalle) {
        try {
            RegistroActividad registro = new RegistroActividad();
            registro.setFecha(LocalDateTime.now(ZONA));
            registro.setAdministrador(recortar(administrador == null || administrador.isBlank() ? "desconocido" : administrador, 150));
            registro.setAccion(recortar(accion, 100));
            registro.setDetalle(recortar(detalle == null ? "" : detalle, 500));
            repository.save(registro);
        } catch (Exception ex) {
            log.warn("No se pudo guardar el registro de actividad ({}): {}", accion, ex.getMessage());
        }
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
