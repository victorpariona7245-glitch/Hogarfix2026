package com.hogarfix.config;

import com.hogarfix.model.Usuario;
import com.hogarfix.model.Rol;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Verificar si el admin ya existe
        if (usuarioRepository.findByEmail("admin@example.com").isPresent()) {
            return; // Ya existe, no hacer nada
        }

        try {
            // Asegurar que existe el rol ADMIN
                Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                    .orElseGet(() -> rolRepository.save(Rol.builder()
                        .nombre("ADMIN")
                        .descripcion("Rol administrador con acceso a dashboard")
                        .build()));

            // Crear usuario admin
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("123456789"))
                    .rol(rolAdmin)
                    .build();

            usuarioRepository.save(admin);
            System.out.println("✓ Admin creado: admin@example.com / 123456789");
        } catch (Exception ex) {
            System.err.println("Error al crear admin: " + ex.getMessage());
        }
    }
}
