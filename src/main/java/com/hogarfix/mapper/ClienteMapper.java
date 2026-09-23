package com.hogarfix.mapper;

import com.hogarfix.dto.ClienteDTO;
import com.hogarfix.model.Cliente;
import com.hogarfix.model.Direccion;
import com.hogarfix.model.Usuario;

public class ClienteMapper {

    public static ClienteDTO toDTO(Cliente entity) {
        if (entity == null)
            return null;

        ClienteDTO dto = new ClienteDTO();
        dto.setIdCliente(entity.getIdCliente());
        dto.setNombres(entity.getNombres());
        dto.setApellidoPaterno(entity.getApellidoPaterno());
        dto.setApellidoMaterno(entity.getApellidoMaterno());
        dto.setDni(entity.getDni());
        dto.setFechaNacimiento(entity.getFechaNacimiento());
        dto.setTelefono(entity.getTelefono());
        dto.setIdUsuario(entity.getUsuario() != null ? entity.getUsuario().getIdUsuario() : null);
        dto.setIdDireccion(entity.getDireccion() != null ? entity.getDireccion().getIdDireccion() : null);
        return dto;
    }

    // Versión toEntity que recibe Usuario y Direccion si necesitas enlazarlos
    public static Cliente toEntity(ClienteDTO dto, Usuario usuario, Direccion direccion) {
        if (dto == null)
            return null;

        Cliente entity = new Cliente();
        entity.setIdCliente(dto.getIdCliente());
        entity.setNombres(dto.getNombres());
        entity.setApellidoPaterno(dto.getApellidoPaterno());
        entity.setApellidoMaterno(dto.getApellidoMaterno());
        entity.setDni(dto.getDni());
        entity.setFechaNacimiento(dto.getFechaNacimiento());
        entity.setTelefono(dto.getTelefono());
        entity.setUsuario(usuario);
        entity.setDireccion(direccion);
        return entity;
    }

    // Si necesitas una sobrecarga simple sin Usuario/Direccion:
    public static Cliente toEntity(ClienteDTO dto) {
        if (dto == null)
            return null;
        Cliente entity = new Cliente();
        entity.setIdCliente(dto.getIdCliente());
        entity.setNombres(dto.getNombres());
        entity.setApellidoPaterno(dto.getApellidoPaterno());
        entity.setApellidoMaterno(dto.getApellidoMaterno());
        entity.setDni(dto.getDni());
        entity.setFechaNacimiento(dto.getFechaNacimiento());
        entity.setTelefono(dto.getTelefono());
        return entity;
    }
}