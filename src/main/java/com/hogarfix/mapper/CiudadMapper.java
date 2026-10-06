package com.hogarfix.mapper;

import com.hogarfix.dto.CiudadDTO;
import com.hogarfix.model.Ciudad;
import com.hogarfix.model.Pais;

public class CiudadMapper {

    public static CiudadDTO toDTO(Ciudad entity) {
        if (entity == null)
            return null;

        CiudadDTO dto = new CiudadDTO();
        dto.setIdCiudad(entity.getIdCiudad());
        dto.setNombre(entity.getNombre());
        dto.setIdPais(entity.getPais() != null ? entity.getPais().getIdPais() : null);
        return dto;
    }

    public static Ciudad toEntity(CiudadDTO dto, Pais pais) {
        if (dto == null)
            return null;

        Ciudad entity = new Ciudad();
        entity.setIdCiudad(dto.getIdCiudad());
        entity.setNombre(dto.getNombre());
        entity.setPais(pais);
        return entity;
    }
}
