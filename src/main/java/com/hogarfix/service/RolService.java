package com.hogarfix.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hogarfix.model.Rol;
import com.hogarfix.repository.RolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    public Optional<Rol> buscarPorId(Long id) {
        return rolRepository.findById(id);
    }

    public Rol registrarRol(Rol rol) {
        return rolRepository.save(rol);
    }

    public Rol actualizarRol(Long id, Rol rolActualizado) {
        return rolRepository.findById(id)
                .map(r -> {
                    r.setNombre(rolActualizado.getNombre());
                    return rolRepository.save(r);
                })
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
    }

    public void eliminarRol(Long id) {
        rolRepository.deleteById(id);
    }
}