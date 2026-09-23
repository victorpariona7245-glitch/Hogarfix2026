package com.hogarfix.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hogarfix.model.Servicio;
import com.hogarfix.repository.ServicioRepository;
import com.hogarfix.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Revisa periódicamente las solicitudes de servicio en estado PENDIENTE.
 * Si el técnico no las acepta ni las rechaza dentro de 15 minutos,
 * se cancelan automáticamente y se notifica al cliente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SolicitudExpiradaScheduler {

    private static final long MINUTOS_LIMITE = 15;

    private final ServicioRepository servicioRepository;
    private final EmailService emailService;

    // Se ejecuta cada minuto
    @Scheduled(fixedRate = 60_000)
    public void cancelarSolicitudesVencidas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_LIMITE);

        List<Servicio> vencidos = servicioRepository.findByEstadoAndFechaSolicitudBefore("PENDIENTE", limite);

        if (vencidos.isEmpty()) {
            return;
        }

        for (Servicio s : vencidos) {
            try {
                s.setEstado("CANCELADO");
                servicioRepository.save(s);

                if (s.getCliente() != null && s.getCliente().getUsuario() != null) {
                    String emailCliente = s.getCliente().getUsuario().getEmail();
                    String nombreCliente = s.getCliente().getNombres();
                    String nombreTecnico = (s.getTecnico() != null)
                            ? s.getTecnico().getNombres() + " " + s.getTecnico().getApellidoPaterno()
                            : null;
                    String categoria = (s.getCategoria() != null) ? s.getCategoria().getNombre() : null;
                    emailService.sendSolicitudExpiradaEmail(emailCliente, nombreCliente, nombreTecnico, categoria);
                }

                log.info("Solicitud cancelada automáticamente por vencimiento: idServicio={}", s.getIdServicio());
            } catch (Exception ex) {
                log.warn("Error al cancelar automáticamente la solicitud id={}: {}", s.getIdServicio(),
                        ex.getMessage());
            }
        }
    }
}