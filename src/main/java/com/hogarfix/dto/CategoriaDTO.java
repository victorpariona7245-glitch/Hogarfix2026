package com.hogarfix.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaDTO {
    private Long idCategoria;
    private String nombre;
}
