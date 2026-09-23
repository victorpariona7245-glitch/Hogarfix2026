package com.hogarfix.dto;

import lombok.Data;

@Data
public class DireccionDTO {
    private Long idDireccion;
    private String calle;
    private String numero;
    private String referencia;
    private Long idCiudad;
}
