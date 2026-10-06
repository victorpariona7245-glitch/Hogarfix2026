package com.hogarfix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

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
                                                                "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                // permitir las rutas de registro (GET y POST explícitamente)
                                                .requestMatchers(HttpMethod.GET, "/clientes/registro",
                                                                "/tecnicos/registro")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/clientes/registro",
                                                                "/tecnicos/registro")
                                                .permitAll()
                                                // --- Solo ADMIN ---
                                                // panel, exportaciones, reportes y documentación de la API
                                                .requestMatchers("/admin/**", "/report/**",
                                                                "/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .hasRole("ADMIN")
                                                // CRUD genérico sin vista en el frontend (listados y altas)
                                                .requestMatchers("/categorias/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/servicios", "/servicios/nuevo",
                                                                "/clientes", "/tecnicos")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/servicios/guardar")
                                                .hasRole("ADMIN")
                                                // --- Endpoints /api que el frontend sí usa: solo requieren login ---
                                                // (cada controlador valida además que el recurso sea del usuario)
                                                .requestMatchers("/api/mensajes/**",
                                                                "/api/pagos/culqi-config",
                                                                "/api/pagos/*/procesar-culqi")
                                                .authenticated()
                                                // el resto de /api (usuarios, roles, pagos, direcciones, etc.) es solo ADMIN
                                                .requestMatchers("/api/**").hasRole("ADMIN")
                                                // el resto de endpoints requieren autenticación
                                                .anyRequest().authenticated())
                                // El login se procesa en AuthController (/auth/login) y TecnicoController
                                // (/tecnicos/login). Se quitó formLogin para no dejar abierto un segundo
                                // endpoint de login (/login_proc) sin límite de intentos; aquí solo se
                                // indica a dónde enviar a quien no ha iniciado sesión.
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                new LoginUrlAuthenticationEntryPoint("/auth/login")))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/auth/login?logout")
                                                .permitAll())
                                // CSRF activo (por defecto)
                                .csrf(Customizer.withDefaults());

                return http.build();
        }
}
