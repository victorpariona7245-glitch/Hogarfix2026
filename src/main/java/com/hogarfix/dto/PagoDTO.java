package com.hogarfix.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDTO {

    private Long idPago;
    private Long idServicio;
    private Long idCliente;

    private BigDecimal monto;
    private String metodoPago;
    private LocalDateTime fechaPago;
    private String estado;
    private String notaRevision;

    // Campos derivados para la vista (evitan exponer entidades JPA en la plantilla)
    private String servicioDescripcion;
    private String categoriaNombre;
    private String tecnicoNombres;
    private String tecnicoTelefono;
    private String tecnicoApellidoPaterno;
    private String tecnicoApellidoMaterno;
    private LocalDateTime servicioFechaFinalizacion;
    private java.math.BigDecimal servicioMonto;

    // Campos de auditoría opcionales (si los deseas mostrar en respuesta)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
