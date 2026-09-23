package com.hogarfix.mapper;

import com.hogarfix.dto.RolDTO;
import com.hogarfix.model.Rol;

public class RolMapper {

    public static RolDTO toDTO(Rol rol) {
        if (rol == null) {
            return null;
        }

        return RolDTO.builder()
                .idRol(rol.getIdRol())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .build();
    }

    public static Rol toEntity(RolDTO dto) {
        if (dto == null) {
            return null;
        }

        return Rol.builder()
                .idRol(dto.getIdRol())
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build();
    }
}