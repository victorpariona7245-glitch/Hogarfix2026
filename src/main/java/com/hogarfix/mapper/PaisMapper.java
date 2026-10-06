package com.hogarfix.mapper;

import com.hogarfix.dto.PaisDTO;
import com.hogarfix.model.Pais;

public class PaisMapper {

    public static PaisDTO toDTO(Pais pais) {
        if (pais == null)
            return null;

        return PaisDTO.builder()
                .idPais(pais.getIdPais())
                .nombre(pais.getNombre())
                .build();
    }

    public static Pais toEntity(PaisDTO dto) {
        if (dto == null)
            return null;

        return Pais.builder()
                .idPais(dto.getIdPais())
                .nombre(dto.getNombre())
                .build();
    }
}
