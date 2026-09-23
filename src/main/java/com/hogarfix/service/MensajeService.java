package com.hogarfix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hogarfix.model.Mensaje;
import com.hogarfix.model.Servicio;
import com.hogarfix.repository.MensajeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;

    public List<Mensaje> listarPorServicio(Servicio servicio) {
        return mensajeRepository.findByServicioOrderByFechaEnvioAsc(servicio);
    }

    public Mensaje enviarMensaje(Servicio servicio, Mensaje.Autor autor, String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new RuntimeException("El mensaje no puede estar vacío");
        }
        if (contenido.length() > 1000) {
            contenido = contenido.substring(0, 1000);
        }
        Mensaje mensaje = Mensaje.builder()
                .servicio(servicio)
                .autor(autor)
                .contenido(contenido.trim())
                .fechaEnvio(LocalDateTime.now())
                .build();
        return mensajeRepository.save(mensaje);
    }
}