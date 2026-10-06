package com.hogarfix.mapper;

import java.util.Collections;

import com.hogarfix.dto.TecnicoDTO;
import com.hogarfix.model.Direccion;
import com.hogarfix.model.Tecnico;
import com.hogarfix.model.Usuario;

public class TecnicoMapper {

    public static TecnicoDTO toDTO(Tecnico entity) {
        if (entity == null)
            return null;

        TecnicoDTO dto = new TecnicoDTO();
        dto.setIdTecnico(entity.getIdTecnico());
        dto.setNombres(entity.getNombres());
        dto.setApellidoPaterno(entity.getApellidoPaterno());
        dto.setApellidoMaterno(entity.getApellidoMaterno());
        dto.setDni(entity.getDni());
        dto.setCertificadoPdf(entity.getCertificadoPdf());
    dto.setTelefono(entity.getTelefono());
    dto.setFotoPerfil(entity.getFotoPerfil());
        dto.setIdUsuario(entity.getUsuario() != null ? entity.getUsuario().getIdUsuario() : null);
        dto.setIdDireccion(entity.getDireccion() != null ? entity.getDireccion().getIdDireccion() : null);
        dto.setPromedioCalificacion(entity.getPromedioCalificacion());
        dto.setCategorias(entity.getCategorias() != null ? entity.getCategorias().stream()
                .map(tc -> tc.getCategoria().getNombre())
                .toList() : Collections.emptyList());
        return dto;
    }

    public static Tecnico toEntity(TecnicoDTO dto, Usuario usuario, Direccion direccion) {
        if (dto == null)
            return null;

        Tecnico entity = new Tecnico();
        entity.setIdTecnico(dto.getIdTecnico());
        entity.setNombres(dto.getNombres());
        entity.setApellidoPaterno(dto.getApellidoPaterno());
        entity.setApellidoMaterno(dto.getApellidoMaterno());
        entity.setDni(dto.getDni());
        entity.setCertificadoPdf(dto.getCertificadoPdf());
    entity.setTelefono(dto.getTelefono());
        entity.setFotoPerfil(dto.getFotoPerfil());
        entity.setUsuario(usuario);
        entity.setDireccion(direccion);
        entity.setPromedioCalificacion(dto.getPromedioCalificacion() != null ? dto.getPromedioCalificacion() : 0.0);
        return entity;
    }
}
