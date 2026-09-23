package com.hogarfix.config;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.hogarfix.model.Ciudad;
import com.hogarfix.model.Pais;
import com.hogarfix.repository.CiudadRepository;
import com.hogarfix.repository.PaisRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final PaisRepository paisRepository;
    private final CiudadRepository ciudadRepository;

    @Override
    public void run(String... args) throws Exception {
        // Crear país Perú si no existe (normalizamos para evitar problemas con acentos)
        var paisOpt = paisRepository.findAll().stream()
                .filter(p -> normalize(p.getNombre()).equals("peru"))
                .findFirst();

        Pais paisPeru = paisOpt.orElseGet(() -> paisRepository.save(Pais.builder().nombre("Perú").build()));

        // Lista de distritos de Lima (al menos 15)
        List<String> limaDistritos = List.of(
                "Cercado de Lima",
                "Miraflores",
                "San Isidro",
                "Barranco",
                "Surco",
                "San Borja",
                "San Miguel",
                "Magdalena del Mar",
                "Pueblo Libre",
                "Jesús María",
                "Lince",
                "La Molina",
                "Ate",
                "San Luis",
                "Breña",
                "El Agustino",
                "Chorrillos"
        );

        // Obtener nombres existentes para el país
        Set<String> existentes = ciudadRepository.findByPaisIdPais(paisPeru.getIdPais()).stream()
                .map(c -> c.getNombre().toLowerCase())
                .collect(Collectors.toSet());

        List<Ciudad> faltantes = limaDistritos.stream()
                .filter(n -> !existentes.contains(n.toLowerCase()))
                .map(n -> Ciudad.builder().nombre(n).pais(paisPeru).build())
                .collect(Collectors.toList());

        if (!faltantes.isEmpty()) {
            ciudadRepository.saveAll(faltantes);
        }
    }

        private String normalize(String input) {
                if (input == null) return "";
                String n = Normalizer.normalize(input, Normalizer.Form.NFD);
                // remove diacritical marks
                return n.replaceAll("\\p{M}", "").toLowerCase();
        }
}
