package com.hogarfix.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hogarfix.model.Pais;
import com.hogarfix.repository.PaisRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaisService {

    private final PaisRepository paisRepository;

    public List<Pais> listarPaises() {
        return paisRepository.findAll();
    }

    public Optional<Pais> buscarPorId(Long id) {
        return paisRepository.findById(id);
    }

    public Pais registrarPais(Pais pais) {
        return paisRepository.save(pais);
    }

    public Pais actualizarPais(Long id, Pais paisActualizado) {
        return paisRepository.findById(id)
                .map(p -> {
                    p.setNombre(paisActualizado.getNombre());
                    return paisRepository.save(p);
                })
                .orElseThrow(() -> new RuntimeException("País no encontrado"));
    }

    public void eliminarPais(Long id) {
        paisRepository.deleteById(id);
    }
}