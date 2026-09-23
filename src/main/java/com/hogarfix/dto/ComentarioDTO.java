package com.hogarfix.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComentarioDTO {
    private Long idComentario;
    private String contenido;
    private LocalDateTime fechaComentario;
    private Long idServicio;
    private Long idCliente;
}