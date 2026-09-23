package com.hogarfix.mapper;

import com.hogarfix.dto.ComentarioDTO;
import com.hogarfix.model.Comentario;

public class ComentarioMapper {

    public static ComentarioDTO toDTO(Comentario entity) {
        if (entity == null)
            return null;

        return ComentarioDTO.builder()
                .idComentario(entity.getIdComentario())
                .contenido(entity.getContenido())
                .fechaComentario(entity.getFechaComentario())
                .idServicio(entity.getServicio() != null ? entity.getServicio().getIdServicio() : null)
                .idCliente(entity.getCliente() != null ? entity.getCliente().getIdCliente() : null)
                .build();
    }

    public static Comentario toEntity(ComentarioDTO dto) {
        if (dto == null)
            return null;

        return Comentario.builder()
                .idComentario(dto.getIdComentario())
                .contenido(dto.getContenido())
                .fechaComentario(dto.getFechaComentario())
                .build();
    }
}
