package com.hogarfix.mapper;

import com.hogarfix.dto.UsuarioDTO;
import com.hogarfix.model.Rol;
import com.hogarfix.model.Usuario;

public class UsuarioMapper {

    public static UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UsuarioDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .password(usuario.getPassword())
                .idRol(usuario.getRol() != null ? usuario.getRol().getIdRol() : null)
                .nombreRol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                // auditoría
                .isActivo(usuario.getIsActivo())               
                .createdAt(usuario.getCreatedAt())
          
                .build();
    }

    public static Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) {
            return null;
        }

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(dto.getIdUsuario());
        usuario.setUsername(dto.getUsername());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(dto.getPassword());

        if (dto.getIdRol() != null) {
            Rol rol = new Rol();
            rol.setIdRol(dto.getIdRol());
            rol.setNombre(dto.getNombreRol());
            usuario.setRol(rol);
        }

        // auditoría
        usuario.setIsActivo(dto.getIsActivo());
        usuario.setCreatedAt(dto.getCreatedAt());

        return usuario;
    }
}