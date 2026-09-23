package com.hogarfix.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.hogarfix.model.Ciudad;
import com.hogarfix.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CiudadService {

    private final CiudadRepository ciudadRepository;

    public List<Ciudad> listarCiudades() {
        return ciudadRepository.findAll();
    }

    public Optional<Ciudad> buscarPorId(Long id) {
        return ciudadRepository.findById(id);
    }

    public Ciudad registrarCiudad(Ciudad ciudad) {
        return ciudadRepository.save(ciudad);
    }

    public Ciudad actualizarCiudad(Long id, Ciudad ciudadActualizada) {
        return ciudadRepository.findById(id)
                .map(c -> {
                    c.setNombre(ciudadActualizada.getNombre());
                    c.setPais(ciudadActualizada.getPais());
                    return ciudadRepository.save(c);
                })
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));
    }

    public void eliminarCiudad(Long id) {
        ciudadRepository.deleteById(id);
    }
}