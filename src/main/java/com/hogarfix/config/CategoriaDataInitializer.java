package com.hogarfix.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.hogarfix.model.Categoria;
import com.hogarfix.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class CategoriaDataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    @Override
    public void run(String... args) throws Exception {
        List<String> categoriasDefault = List.of(
                "Plomería",
                "Electricidad",
                "Reparación de Electrodomésticos",
                "Instalaciones A/C",
                "Mantenimiento General",
                "Emergencias 24/7",
                "Carpintería",
                "Cerrajería",
                "Pintura"
        );

        Set<String> existentes = categoriaRepository.findAll().stream()
                .map(c -> c.getNombre().toLowerCase())
                .collect(Collectors.toSet());

        List<Categoria> faltantes = categoriasDefault.stream()
                .filter(n -> !existentes.contains(n.toLowerCase()))
                .map(n -> Categoria.builder().nombre(n).build())
                .collect(Collectors.toList());

        if (!faltantes.isEmpty()) {
            categoriaRepository.saveAll(faltantes);
        }
    }
}
