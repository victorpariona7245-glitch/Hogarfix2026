package com.hogarfix.config;

import com.hogarfix.model.Usuario;
import com.hogarfix.model.Rol;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyección de credenciales mediante variables de entorno
    @Value("${app.admin.email:admin@hogarfix.pe}")
    private String adminEmail;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:${ADMIN_DEFAULT_PASSWORD:HogarFix2026*AdminSecure}}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Verificar si el admin ya existe
        if (usuarioRepository.findByEmail(adminEmail).isPresent()) {
            return; // Ya existe, no hacer nada
        }

        try {
            // Asegurar que existe el rol ADMIN
            Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(Rol.builder()
                    .nombre("ADMIN")
                    .descripcion("Rol administrador con acceso a dashboard")
                    .build()));

            // Crear usuario admin con credenciales parametrizadas
            Usuario admin = Usuario.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .rol(rolAdmin)
                    .build();

            usuarioRepository.save(admin);
            
            // Usar Logger profesional y NUNCA imprimir la contraseña en texto plano
            log.info("✓ Usuario administrador inicial inicializado con éxito ({})", adminEmail);
        } catch (Exception ex) {
            log.error("Error al crear el usuario administrador inicial: {}", ex.getMessage());
        }
    }
}
