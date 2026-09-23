package com.hogarfix.config;

import com.hogarfix.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private final UsuarioRepository usuarioRepository;

    @Bean
    public UserDetailsService userDetailsService() {
    return username -> {
        try {
            // Intentar buscar por email primero, si no, por username
            var usuario = usuarioRepository.findByEmail(username)
                .or(() -> usuarioRepository.findByUsername(username));
            
            if (usuario.isPresent()) {
                logger.debug("Usuario encontrado en BD: {}", username);
                return usuario.get();
            } else {
                logger.warn("Usuario no encontrado en BD: {}", username);
                throw new UsernameNotFoundException("Usuario no encontrado: " + username);
            }
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al buscar usuario en BD: {}", username, e);
            throw new UsernameNotFoundException("Error al buscar usuario en BD", e);
        }
    };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}