package com.hogarfix.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalificacionDTO {

    private Long idCalificacion;
    private int puntuacion;
    private String observacion;
    private LocalDateTime fechaCalificacion;
    private Long idServicio;
}
