package com.hogarfix.mapper;

import com.hogarfix.dto.CalificacionDTO;
import com.hogarfix.model.Calificacion;
import com.hogarfix.model.Servicio;

public class CalificacionMapper {

    public static CalificacionDTO toDTO(Calificacion entity) {
        if (entity == null)
            return null;

        return CalificacionDTO.builder()
                .idCalificacion(entity.getIdCalificacion())
                .puntuacion(entity.getPuntuacion())
                .observacion(entity.getObservacion())
                .fechaCalificacion(entity.getFechaCalificacion())
                .idServicio(entity.getServicio() != null ? entity.getServicio().getIdServicio() : null)
                .build();
    }

    public static Calificacion toEntity(CalificacionDTO dto, Servicio servicio) {
        if (dto == null)
            return null;

        return Calificacion.builder()
                .idCalificacion(dto.getIdCalificacion())
                .puntuacion(dto.getPuntuacion())
                .observacion(dto.getObservacion())
                .fechaCalificacion(dto.getFechaCalificacion())
                .servicio(servicio)
                .build();
    }
}
