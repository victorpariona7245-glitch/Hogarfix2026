package com.hogarfix.mapper;

import com.hogarfix.dto.CategoriaDTO;
import com.hogarfix.model.Categoria;

public class CategoriaMapper {

    public static CategoriaDTO toDTO(Categoria entity) {
        if (entity == null)
            return null;

        return CategoriaDTO.builder()
                .idCategoria(entity.getIdCategoria())
                .nombre(entity.getNombre())
                .build();
    }

    public static Categoria toEntity(CategoriaDTO dto) {
        if (dto == null)
            return null;

        return Categoria.builder()
                .idCategoria(dto.getIdCategoria())
                .nombre(dto.getNombre())
                .build();
    }
}