package com.hogarfix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String nombre) {
        if (to == null || to.isBlank()) return;

        String subject = "Bienvenido a HogarFix";
        String text = "Hola " + (nombre != null && !nombre.isBlank() ? nombre : "") + ",\n\n"
                + "Gracias por registrarte en HogarFix. Nos alegra darte la bienvenida y esperamos ayudarte a conectar con los mejores técnicos para tu hogar.\n\n"
                + "Saludos,\nEl equipo de HogarFix";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            // Log error but don't prevent flow
            System.err.println("No se pudo enviar email de bienvenida a " + to + ": " + ex.getMessage());
        }
    }

    /**
     * Notifica al cliente que un técnico rechazó su solicitud de servicio.
     */
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

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("No se pudo enviar email de rechazo a " + to + ": " + ex.getMessage());
        }
    }

    /**
     * Notifica al técnico que le llegó una nueva solicitud de servicio.
     */
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

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("No se pudo enviar email de nueva solicitud a " + to + ": " + ex.getMessage());
        }
    }

    /**
     * Notifica al cliente que su solicitud fue cancelada automáticamente
     * porque el técnico no la aceptó ni la rechazó dentro del tiempo límite.
     */
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

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("No se pudo enviar email de solicitud expirada a " + to + ": " + ex.getMessage());
        }
    }
}