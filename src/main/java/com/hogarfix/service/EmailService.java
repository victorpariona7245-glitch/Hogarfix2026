package com.hogarfix.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envío de correos.
 * - Si existe RESEND_API_KEY, envía por la API HTTP de Resend (funciona en Render y Railway,
 *   donde el SMTP está bloqueado).
 * - Si no, usa el SMTP configurado en spring.mail.* (por ejemplo Gmail en tu PC).
 * Todos los métodos son asíncronos y nunca interrumpen el flujo si el correo falla.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final JavaMailSender mailSender;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${mail.from:onboarding@resend.dev}")
    private String remitente;

    private final RestTemplate restTemplate = crearRestTemplate();

    private static RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        return new RestTemplate(factory);
    }

    /** Envía un correo de texto. Devuelve sin lanzar excepción aunque falle. */
    private void enviar(String to, String subject, String text, String tipo) {
        try {
            if (resendApiKey != null && !resendApiKey.isBlank()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(resendApiKey.trim());
                Map<String, Object> cuerpo = Map.of(
                        "from", remitente,
                        "to", List.of(to),
                        "subject", subject,
                        "text", text);
                restTemplate.postForEntity(RESEND_URL, new HttpEntity<>(cuerpo, headers), String.class);
                log.info("Email de {} enviado a {} con Resend", tipo, to);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                log.info("Email de {} enviado a {} por SMTP", tipo, to);
            }
        } catch (HttpStatusCodeException ex) {
            log.warn("Resend rechazó el email de {} a {}: {} {}", tipo, to, ex.getStatusCode(),
                    ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("No se pudo enviar email de {} a {}: {}", tipo, to, ex.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String nombre) {
        if (to == null || to.isBlank()) return;

        String subject = "Bienvenido a HogarFix";
        String text = "Hola " + (nombre != null && !nombre.isBlank() ? nombre : "") + ",\n\n"
                + "Gracias por registrarte en HogarFix. Nos alegra darte la bienvenida y esperamos ayudarte a conectar con los mejores técnicos para tu hogar.\n\n"
                + "Saludos,\nEl equipo de HogarFix";
        enviar(to, subject, text, "bienvenida");
    }

    /**
     * Notifica al cliente que un técnico rechazó su solicitud de servicio.
     */
    @Async
    public void sendSolicitudRechazadaEmail(String to, String nombreCliente, String nombreTecnico,
            String categoria) {
        if (to == null || to.isBlank()) return;

        String subject = "Tu solicitud de servicio fue rechazada";
        String text = "Hola " + (nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : "") + ",\n\n"
                + "Te informamos que el técnico" + (nombreTecnico != null ? " " + nombreTecnico : "")
                + " no pudo aceptar tu solicitud de servicio"
                + (categoria != null ? " de " + categoria : "") + ".\n\n"
                + "No te preocupes, puedes ingresar a HogarFix y buscar otro técnico disponible para tu solicitud.\n\n"
                + "Saludos,\nEl equipo de HogarFix";
        enviar(to, subject, text, "rechazo");
    }

    /**
     * Notifica al técnico que le llegó una nueva solicitud de servicio.
     */
    @Async
    public void sendNuevaSolicitudEmail(String to, String nombreTecnico, String nombreCliente, String categoria,
            String urgencia, String descripcion) {
        if (to == null || to.isBlank()) return;

        String subject = "Nueva solicitud de servicio en HogarFix";
        String text = "Hola " + (nombreTecnico != null && !nombreTecnico.isBlank() ? nombreTecnico : "") + ",\n\n"
                + "Tienes una nueva solicitud de servicio"
                + (categoria != null ? " de " + categoria : "") + ".\n\n"
                + "Cliente: " + (nombreCliente != null ? nombreCliente : "N/A") + "\n"
                + "Urgencia: " + (urgencia != null ? urgencia : "N/A") + "\n"
                + "Descripción: " + (descripcion != null ? descripcion : "N/A") + "\n\n"
                + "Ingresa a tu panel en HogarFix para aceptar o rechazar la solicitud.\n\n"
                + "Saludos,\nEl equipo de HogarFix";
        enviar(to, subject, text, "nueva solicitud");
    }

    /**
     * Notifica al cliente que su solicitud fue cancelada automáticamente
     * porque el técnico no la aceptó ni la rechazó dentro del tiempo límite.
     */
    @Async
    public void sendSolicitudExpiradaEmail(String to, String nombreCliente, String nombreTecnico,
            String categoria) {
        if (to == null || to.isBlank()) return;

        String subject = "Tu solicitud de servicio fue cancelada";
        String text = "Hola " + (nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : "") + ",\n\n"
                + "Tu solicitud de servicio" + (categoria != null ? " de " + categoria : "")
                + (nombreTecnico != null ? " con " + nombreTecnico : "")
                + " fue cancelada automáticamente porque no recibió respuesta a tiempo (15 minutos).\n\n"
                + "Puedes ingresar a HogarFix y solicitar el servicio nuevamente con otro técnico disponible.\n\n"
                + "Saludos,\nEl equipo de HogarFix";
        enviar(to, subject, text, "solicitud expirada");
    }
}
