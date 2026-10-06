package com.hogarfix.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioDTO {
    private Long idServicio;
    private Long idCliente;
    private Long idTecnico;
    private Long idCategoria;
    private String descripcion;
    private BigDecimal monto;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaFinalizacion;
    private Integer numSolicitudes;
}
