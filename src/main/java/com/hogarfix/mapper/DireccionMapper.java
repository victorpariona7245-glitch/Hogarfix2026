package com.hogarfix.mapper;

import com.hogarfix.dto.DireccionDTO;
import com.hogarfix.model.Ciudad;
import com.hogarfix.model.Direccion;

public class DireccionMapper {

    public static DireccionDTO toDTO(Direccion entity) {
        if (entity == null)
            return null;

        DireccionDTO dto = new DireccionDTO();
        dto.setIdDireccion(entity.getIdDireccion());
        dto.setCalle(entity.getCalle());
        dto.setNumero(entity.getNumero());
        dto.setReferencia(entity.getReferencia());
        dto.setIdCiudad(entity.getCiudad() != null ? entity.getCiudad().getIdCiudad() : null);
        return dto;
    }

    public static Direccion toEntity(DireccionDTO dto, Ciudad ciudad) {
        if (dto == null)
            return null;

        Direccion entity = new Direccion();
        entity.setIdDireccion(dto.getIdDireccion());
        entity.setCalle(dto.getCalle());
        entity.setNumero(dto.getNumero());
        entity.setReferencia(dto.getReferencia());
        entity.setCiudad(ciudad);
        return entity;
    }
}