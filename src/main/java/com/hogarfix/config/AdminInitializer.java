package com.hogarfix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.Rol;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.RolRepository;
import com.hogarfix.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea el administrador inicial automáticamente al arrancar, sin configurar nada:
 * admin@example.com / 123456789 (igual que en la versión anterior).
 *
 * Opcional: si defines las variables de entorno ADMIN_EMAIL, ADMIN_USERNAME o ADMIN_PASSWORD, se usan esas.
 * Si defines ADMIN_PASSWORD y el admin ya existe con otra contraseña, se actualiza (así puedes endurecerla
 * más adelante sin tocar la base de datos).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private static final String EMAIL_POR_DEFECTO = "admin@example.com";
    private static final String USERNAME_POR_DEFECTO = "admin";
    private static final String PASSWORD_POR_DEFECTO = "123456789";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmailConfig;

    @Value("${app.admin.username:}")
    private String adminUsernameConfig;

    @Value("${app.admin.password:}")
    private String adminPasswordConfig;

    @Override
    @Transactional
    public void run(String... args) {
        String email = vacio(adminEmailConfig) ? EMAIL_POR_DEFECTO : adminEmailConfig;
        String username = vacio(adminUsernameConfig) ? USERNAME_POR_DEFECTO : adminUsernameConfig;
        boolean passwordConfigurada = !vacio(adminPasswordConfig);
        String password = passwordConfigurada ? adminPasswordConfig : PASSWORD_POR_DEFECTO;

        try {
            var existente = usuarioRepository.findByEmail(email);
            if (existente.isPresent()) {
                // Ya existe: no se toca, salvo que hayas definido ADMIN_PASSWORD y sea distinta de la guardada
                Usuario admin = existente.get();
                if (passwordConfigurada && !passwordEncoder.matches(password, admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode(password));
                    usuarioRepository.save(admin);
                    log.info("Contraseña del administrador {} actualizada con ADMIN_PASSWORD.", email);
                }
                return;
            }

            Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                    .orElseGet(() -> rolRepository.save(Rol.builder()
                            .nombre("ADMIN")
                            .descripcion("Rol administrador con acceso a dashboard")
                            .build()));

            Usuario admin = Usuario.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .rol(rolAdmin)
                    .build();

            usuarioRepository.save(admin);
            log.info("Usuario administrador inicial creado: {}", email);
        } catch (Exception ex) {
            log.error("Error al crear el administrador inicial: {}", ex.getMessage());
        }
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
