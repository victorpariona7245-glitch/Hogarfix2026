package com.hogarfix.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hogarfix.model.Direccion;
import com.hogarfix.repository.DireccionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DireccionService {

    private final DireccionRepository direccionRepository;

    public List<Direccion> listarDirecciones() {
        return direccionRepository.findAll();
    }

    public Optional<Direccion> buscarPorId(Long id) {
        return direccionRepository.findById(id);
    }

    public Direccion registrarDireccion(Direccion direccion) {
        return direccionRepository.save(direccion);
    }

    public Direccion actualizarDireccion(Long id, Direccion direccionActualizada) {
        return direccionRepository.findById(id)
                .map(d -> {
                    d.setCalle(direccionActualizada.getCalle());
                    d.setNumero(direccionActualizada.getNumero());
                    d.setCiudad(direccionActualizada.getCiudad());
                    return direccionRepository.save(d);
                })
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
    }

    public void eliminarDireccion(Long id) {
        direccionRepository.deleteById(id);
    }
}