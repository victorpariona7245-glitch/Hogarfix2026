package com.hogarfix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.sameOrigin()))
                                .authorizeHttpRequests(auth -> auth
                                                // recursos públicos y páginas de autenticación/registro
                                                .requestMatchers(
                                                                "/", "/auth/**", "/tecnicos/login",
                                                                "/tecnicos/registro", "/tecnicos/registro-exitoso",
                                                                "/clientes/registro",
                                                                "/css/**", "/js/**", "/images/**", "/uploads/**")
                                                .permitAll()
                                                // permitir las rutas de registro (GET y POST explícitamente)
                                                .requestMatchers(HttpMethod.GET, "/clientes/registro",
                                                                "/tecnicos/registro")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/clientes/registro",
                                                                "/tecnicos/registro")
                                                .permitAll()
                                                // proteger rutas admin solo para usuarios con rol ADMIN
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                // el resto de endpoints requieren autenticación
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/auth/login") // tu login personalizado
                                                // No interceptamos el POST en /auth/login porque el controlador lo
                                                // procesa programáticamente
                                                // Usamos una URL de procesamiento diferente para el filtro de Spring
                                                // Security
                                                .loginProcessingUrl("/login_proc")
                                                // permitir redirigir al destino original en vez de forzar siempre '/'
                                                .defaultSuccessUrl("/", false)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/auth/login?logout")
                                                .permitAll())
                                // Deshabilitar CSRF solo en desarrollo o si manejas tokens manualmente
                                .csrf(csrf -> csrf.disable());

                return http.build();
        }
}