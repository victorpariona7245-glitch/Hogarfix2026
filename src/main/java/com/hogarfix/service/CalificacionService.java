package com.hogarfix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.Calificacion;
import com.hogarfix.model.Cliente;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;
import com.hogarfix.repository.CalificacionRepository;
import com.hogarfix.repository.TecnicoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final TecnicoRepository tecnicoRepository;

    public Calificacion registrarCalificacion(Calificacion calificacion) {
        return calificacionRepository.save(calificacion);
    }

    public List<Calificacion> listarCalificaciones() {
        return calificacionRepository.findAll();
    }

    /**
     * Registra la calificación que un cliente da a un servicio ya finalizado,
     * y actualiza automáticamente el promedio de calificación del técnico.
     */
    @Transactional
    public Calificacion calificarServicio(Servicio servicio, Cliente clienteAutenticado, int puntuacion,
            String observacion) {

        // Validar que el servicio pertenece al cliente autenticado
        if (servicio.getCliente() == null
                || !servicio.getCliente().getIdCliente().equals(clienteAutenticado.getIdCliente())) {
            throw new RuntimeException("No tienes permiso para calificar este servicio");
        }

        // Solo se puede calificar un servicio finalizado
        if (servicio.getEstado() == null || !servicio.getEstado().equalsIgnoreCase("FINALIZADO")) {
            throw new RuntimeException("Solo puedes calificar servicios finalizados");
        }

        // Evitar calificar dos veces el mismo servicio
        List<Calificacion> existentes = calificacionRepository.findByServicio(servicio);
        if (existentes != null && !existentes.isEmpty()) {
            throw new RuntimeException("Este servicio ya fue calificado");
        }

        // Validar rango de puntuación
        if (puntuacion < 1 || puntuacion > 5) {
            throw new RuntimeException("La puntuación debe estar entre 1 y 5");
        }

        if (servicio.getTecnico() == null) {
            throw new RuntimeException("El servicio no tiene técnico asignado");
        }

        Calificacion calificacion = Calificacion.builder()
                .servicio(servicio)
                .puntuacion(puntuacion)
                .observacion(observacion)
                .fechaCalificacion(LocalDateTime.now())
                .build();

        Calificacion guardada = calificacionRepository.save(calificacion);

        actualizarPromedioTecnico(servicio.getTecnico());

        log.info("Calificación registrada: idServicio={}, puntuacion={}, idTecnico={}",
                servicio.getIdServicio(), puntuacion, servicio.getTecnico().getIdTecnico());

        return guardada;
    }

    /**
     * Recalcula y guarda el promedio de calificación de un técnico,
     * en base a TODAS sus calificaciones existentes.
     */
    private void actualizarPromedioTecnico(Tecnico tecnico) {
        Double promedio = calificacionRepository.promedioPorTecnico(tecnico);
        tecnico.setPromedioCalificacion(promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0);
        tecnicoRepository.save(tecnico);
    }
}