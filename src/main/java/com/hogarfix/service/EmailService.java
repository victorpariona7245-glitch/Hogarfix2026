package com.hogarfix.service;

import java.util.List;
import java.util.Map;

import org.springframework.mail.javamail.MimeMessageHelper;

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
 * Envío de correos:
 * 1. Si existe BREVO_API_KEY, usa la API HTTP de Brevo (funciona en Railway, donde el SMTP está bloqueado).
 * 2. Si no, usa el SMTP de spring.mail.* (por ejemplo Gmail en tu PC).
 * Todos los métodos son asíncronos y nunca interrumpen el flujo si el correo falla.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:}")
    private String brevoSenderEmail;

    @Value("${brevo.sender.name:HogarFix}")
    private String brevoSenderName;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

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
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                if (brevoSenderEmail == null || brevoSenderEmail.isBlank()) {
                    log.warn("Falta BREVO_SENDER_EMAIL: no se envió el email de {} a {}", tipo, to);
                    return;
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey.trim());
                Map<String, Object> cuerpo = Map.of(
                        "sender", Map.of("name", brevoSenderName, "email", brevoSenderEmail.trim()),
                        "to", List.of(Map.of("email", to)),
                        "subject", subject,
                        "textContent", text);
                restTemplate.postForEntity(BREVO_URL, new HttpEntity<>(cuerpo, headers), String.class);
                log.info("Email de {} enviado a {} con Brevo", tipo, to);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                log.info("Email de {} enviado a {} por SMTP", tipo, to);
            }
        } catch (HttpStatusCodeException ex) {
            log.warn("Brevo rechazó el email de {} a {}: {} {}", tipo, to, ex.getStatusCode(),
                    ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("No se pudo enviar email de {} a {}: {}", tipo, to, ex.getMessage());
        }
    }

    /** Envía un correo con HTML (y versión de texto de respaldo). Nunca lanza excepción. */
    private void enviarHtml(String to, String subject, String html, String text, String tipo) {
        try {
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                if (brevoSenderEmail == null || brevoSenderEmail.isBlank()) {
                    log.warn("Falta BREVO_SENDER_EMAIL: no se envió el email de {} a {}", tipo, to);
                    return;
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey.trim());
                Map<String, Object> cuerpo = Map.of(
                        "sender", Map.of("name", brevoSenderName, "email", brevoSenderEmail.trim()),
                        "to", List.of(Map.of("email", to)),
                        "subject", subject,
                        "htmlContent", html,
                        "textContent", text);
                restTemplate.postForEntity(BREVO_URL, new HttpEntity<>(cuerpo, headers), String.class);
                log.info("Email de {} enviado a {} con Brevo", tipo, to);
            } else {
                var mime = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(text, html);
                mailSender.send(mime);
                log.info("Email de {} enviado a {} por SMTP", tipo, to);
            }
        } catch (HttpStatusCodeException ex) {
            log.warn("Brevo rechazó el email de {} a {}: {} {}", tipo, to, ex.getStatusCode(),
                    ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("No se pudo enviar email de {} a {}: {}", tipo, to, ex.getMessage());
        }
    }

    private static String esc(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Envía al cliente su boleta del servicio (constancia) al correo con el que se registró. */
    @Async
    public void sendBoletaEmail(String to, String nombreCliente, Long idServicio, String numero,
            String fechaServicio, String fechaPago, String tecnico, String categoria, String descripcion,
            String metodo, String monto) {
        if (to == null || to.isBlank()) return;

        String enlace = baseUrl + "/boletas/" + idServicio;
        String logo = baseUrl + "/images/logo.png";
        String saludo = (nombreCliente != null && !nombreCliente.isBlank()) ? nombreCliente : "";

        String subject = "Tu boleta de HogarFix " + numero;
        String text = "Hola " + saludo + ",\n\n"
                + "Gracias por confiar en HogarFix. Esta es la boleta de tu servicio.\n\n"
                + "Boleta N° " + numero + "\n"
                + "Servicio: " + categoria + (descripcion != null ? " - " + descripcion : "") + "\n"
                + "Técnico: " + tecnico + "\n"
                + "Fecha del servicio: " + fechaServicio + "\n"
                + "Fecha de pago: " + fechaPago + "\n"
                + "Método de pago: " + metodo + "\n"
                + "Total: S/ " + monto + "\n\n"
                + "Puedes verla, imprimirla o guardarla en PDF aquí:\n" + enlace + "\n\n"
                + "Este documento es una constancia del servicio y no constituye comprobante de pago electrónico de SUNAT.\n\n"
                + "Saludos,\nEl equipo de HogarFix";

        String fila = "<tr><td style=\"padding:6px 0;color:#64748b;font-size:13px;\">%s</td>"
                + "<td style=\"padding:6px 0;text-align:right;font-size:14px;font-weight:500;\">%s</td></tr>";
        String html = "<div style=\"background:#f1f5f9;padding:24px 12px;font-family:Arial,Helvetica,sans-serif;color:#0f172a;\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;padding:28px;\">"
                + "<table style=\"width:100%;border-bottom:2px solid #030213;padding-bottom:14px;\"><tr>"
                + "<td><img src=\"" + esc(logo) + "\" alt=\"HogarFix\" width=\"48\" height=\"48\" style=\"vertical-align:middle;\" />"
                + "<span style=\"font-size:20px;font-weight:bold;margin-left:8px;vertical-align:middle;\">HogarFix</span></td>"
                + "<td style=\"text-align:right;\"><div style=\"font-size:11px;color:#64748b;text-transform:uppercase;\">Boleta de servicio</div>"
                + "<div style=\"font-size:16px;font-weight:bold;\">N° " + esc(numero) + "</div></td></tr></table>"
                + "<p style=\"font-size:15px;margin:20px 0 4px;\">Hola " + esc(saludo) + ",</p>"
                + "<p style=\"font-size:14px;color:#475569;margin:0 0 16px;\">Gracias por confiar en HogarFix. Esta es la boleta de tu servicio.</p>"
                + "<table style=\"width:100%;border-collapse:collapse;\">"
                + String.format(fila, "Servicio", esc(categoria))
                + String.format(fila, "Detalle", esc(descripcion))
                + String.format(fila, "Técnico", esc(tecnico))
                + String.format(fila, "Fecha del servicio", esc(fechaServicio))
                + String.format(fila, "Fecha de pago", esc(fechaPago))
                + String.format(fila, "Método de pago", esc(metodo))
                + "</table>"
                + "<div style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin:16px 0;"
                + "display:flex;justify-content:space-between;\"><table style=\"width:100%;\"><tr>"
                + "<td style=\"font-size:16px;font-weight:bold;\">Total</td>"
                + "<td style=\"text-align:right;font-size:18px;font-weight:bold;\">S/ " + esc(monto) + "</td></tr></table></div>"
                + "<p style=\"text-align:center;margin:22px 0;\"><a href=\"" + esc(enlace) + "\" "
                + "style=\"background:#030213;color:#ffffff;text-decoration:none;padding:12px 22px;border-radius:10px;font-size:14px;font-weight:bold;\">"
                + "Ver, imprimir o guardar en PDF</a></p>"
                + "<p style=\"font-size:11px;color:#94a3b8;text-align:center;line-height:1.6;margin:0;\">"
                + "Este documento es una constancia del servicio prestado a través de HogarFix y no constituye "
                + "comprobante de pago electrónico de SUNAT.</p>"
                + "</div></div>";
        enviarHtml(to, subject, html, text, "boleta");
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

    /**
     * Envía el enlace para restablecer la contraseña (vale 30 minutos).
     */
    @Async
    public void sendRecuperarPasswordEmail(String to, String nombre, String enlace) {
        if (to == null || to.isBlank()) return;

        String subject = "Restablece tu contraseña de HogarFix";
        String text = "Hola " + (nombre != null && !nombre.isBlank() ? nombre : "") + ",\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta en HogarFix.\n\n"
                + "Haz clic en este enlace para crear una contraseña nueva (vale 30 minutos y solo se puede usar una vez):\n"
                + enlace + "\n\n"
                + "Si no fuiste tú, ignora este correo: tu contraseña actual no cambiará.\n\n"
                + "Saludos,\nEl equipo de HogarFix";
        enviar(to, subject, text, "recuperar contraseña");
    }
}
