package com.hogarfix.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Listener que se ejecuta cuando la aplicación termina de inicializarse.
 * Registra logs de confirmación de lanzamiento exitoso.
 */
@Component
public class ApplicationStartupListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);
    
    private final Environment environment;
    
    public ApplicationStartupListener(Environment environment) {
        this.environment = environment;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String appName = environment.getProperty("spring.application.name", "HogarFix");
        String profile = String.join(",", environment.getActiveProfiles().length > 0 
            ? environment.getActiveProfiles() 
            : new String[]{"default"});
        String port = environment.getProperty("server.port", "8080");
        
        logger.info("================================================================");
        logger.info("[OK] {} lanzado exitosamente", appName);
        logger.info("[OK] Perfil activo: {}", profile);
        logger.info("[OK] Puerto: {}", port);
        logger.info("[OK] La aplicacion esta lista para recibir solicitudes");
        logger.info("================================================================");
    }
}
