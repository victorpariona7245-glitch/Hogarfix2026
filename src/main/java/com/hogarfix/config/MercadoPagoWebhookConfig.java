package com.hogarfix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * MercadoPago llama a /pagos/mp-webhook sin sesión y sin token CSRF, por eso se deja fuera de la cadena
 * de seguridad. Es seguro porque el controlador NO confía en lo que recibe: vuelve a consultar el pago
 * directamente a la API de MercadoPago y comprueba referencia, monto y moneda antes de marcar nada.
 */
@Configuration
public class MercadoPagoWebhookConfig {

    @Bean
    public WebSecurityCustomizer mercadoPagoWebhookSinSeguridad() {
        return web -> web.ignoring().requestMatchers("/pagos/mp-webhook");
    }
}
