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
public class UsuarioDTO {

    private Long idUsuario;
    private String username;
    private String email;
    private String password;
    private Long idRol;
    private String nombreRol;

    // Campos de auditoría
    private Boolean isActivo;
    private Integer usuarioCreacion;
    private Integer usuarioModificacion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}