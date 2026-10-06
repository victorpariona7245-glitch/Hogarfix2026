package com.hogarfix.mapper;

import com.hogarfix.dto.ServicioDTO;
import com.hogarfix.model.Servicio;

public class ServicioMapper {

    public static ServicioDTO toDTO(Servicio servicio) {
        if (servicio == null) {
            return null;
        }

        return ServicioDTO.builder()
                .idServicio(servicio.getIdServicio())
                .idCliente(servicio.getCliente() != null ? servicio.getCliente().getIdCliente() : null)
                .idTecnico(servicio.getTecnico() != null ? servicio.getTecnico().getIdTecnico() : null)
                .idCategoria(servicio.getCategoria() != null ? servicio.getCategoria().getIdCategoria() : null)
                .descripcion(servicio.getDescripcion())
                .monto(servicio.getMonto())
                .estado(servicio.getEstado())
                .fechaSolicitud(servicio.getFechaSolicitud())
                .fechaFinalizacion(servicio.getFechaFinalizacion())
                .numSolicitudes(servicio.getNumSolicitudes())
                .build();
    }

    public static Servicio toEntity(ServicioDTO dto) {
        if (dto == null) {
            return null;
        }

        return Servicio.builder()
                .idServicio(dto.getIdServicio())
                .descripcion(dto.getDescripcion())
                .monto(dto.getMonto())
                .estado(dto.getEstado())
                .fechaSolicitud(dto.getFechaSolicitud())
                .fechaFinalizacion(dto.getFechaFinalizacion())
                .numSolicitudes(dto.getNumSolicitudes())
                .build();
    }
}