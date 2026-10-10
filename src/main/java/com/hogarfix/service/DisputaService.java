package com.hogarfix.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.Disputa;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.DisputaRepository;
import com.hogarfix.repository.ServicioRepository;
import com.hogarfix.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Disputas: cliente o técnico reportan un problema y el admin lo resuelve. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputaService {

    public static final Set<String> MOTIVOS = Set.of(
            "NO_LLEGO", "TRABAJO_INCOMPLETO", "COBRO_INCORRECTO", "PAGO_NO_RECIBIDO", "MAL_TRATO", "OTRO");
    public static final Set<String> DECISIONES = Set.of("A_FAVOR_CLIENTE", "A_FAVOR_TECNICO", "ACUERDO");
    private static final Set<String> ESTADOS_REPORTABLES = Set.of("ACEPTADO", "EN_CAMINO", "EN_PROCESO", "FINALIZADO");

    private final DisputaRepository disputaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    /** Resultado de abrir una disputa: a qué pantalla volver. */
    public record Apertura(Disputa disputa, boolean esTecnico) {}

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class FilaDisputa {
        private final Long id;
        private final Long idServicio;
        private final LocalDateTime fecha;
        private final String rolAbre;
        private final String abiertaPor;
        private final String cliente;
        private final String tecnico;
        private final String categoria;
        private final String servicioDescripcion;
        private final String monto;
        private final String metodoPago;
        private final String estadoPago;
        private final String motivo;
        private final String descripcion;
        private final String estado;
        private final String decision;
        private final String resolucion;
        private final LocalDateTime fechaResolucion;
    }

    @Transactional(readOnly = true)
    public boolean hayAbierta(Long idServicio) {
        return idServicio != null && disputaRepository.existsByServicio_IdServicioAndEstado(idServicio, "ABIERTA");
    }

    @Transactional
    public Apertura abrir(Long idServicio, String login, String motivo, String descripcion) {
        Usuario u = notificacionService.usuarioPorLogin(login)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        Servicio s = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new IllegalStateException("Servicio no encontrado"));

        boolean esCliente = s.getCliente() != null && s.getCliente().getUsuario() != null
                && s.getCliente().getUsuario().getIdUsuario().equals(u.getIdUsuario());
        boolean esTecnico = s.getTecnico() != null && s.getTecnico().getUsuario() != null
                && s.getTecnico().getUsuario().getIdUsuario().equals(u.getIdUsuario());
        if (!esCliente && !esTecnico) throw new IllegalStateException("No puedes reportar este servicio");
        if (s.getEstado() == null || !ESTADOS_REPORTABLES.contains(s.getEstado())) {
            throw new IllegalStateException("Este servicio ya no admite reportes");
        }
        if (motivo == null || !MOTIVOS.contains(motivo)) throw new IllegalStateException("Elige un motivo");
        String desc = descripcion == null ? "" : descripcion.trim();
        if (desc.length() < 10) throw new IllegalStateException("Cuéntanos qué pasó (mínimo 10 letras)");
        if (desc.length() > 600) desc = desc.substring(0, 600);
        if (disputaRepository.existsByServicio_IdServicioAndEstado(idServicio, "ABIERTA")) {
            throw new IllegalStateException("Ya hay un reporte abierto para este servicio");
        }

        Disputa d = new Disputa();
        d.setServicio(s);
        d.setAbiertaPor(u);
        d.setRolAbre(esTecnico ? "TECNICO" : "CLIENTE");
        d.setMotivo(motivo);
        d.setDescripcion(desc);
        d = disputaRepository.save(d);

        try {
            String quien = esTecnico ? "El técnico" : "El cliente";
            for (Usuario admin : usuarioRepository.findByRol_NombreIgnoreCase("ADMIN")) {
                notificacionService.crear(admin, "DISPUTA", quien + " reportó un problema en el servicio #"
                        + idServicio + ". Revísalo", "/admin/disputas", d.getIdDisputa());
            }
            Usuario otro = esTecnico
                    ? (s.getCliente() != null ? s.getCliente().getUsuario() : null)
                    : (s.getTecnico() != null ? s.getTecnico().getUsuario() : null);
            notificacionService.crear(otro, "DISPUTA", quien + " reportó un problema en el servicio #" + idServicio
                    + ". HogarFix lo revisará y te avisará", esTecnico ? "/clientes/perfil" : "/tecnicos/panel",
                    d.getIdDisputa());
        } catch (Exception ex) {
            log.warn("No se pudo avisar de la disputa {}: {}", d.getIdDisputa(), ex.getMessage());
        }
        return new Apertura(d, esTecnico);
    }

    @Transactional
    public Disputa resolver(Long id, String decision, String nota, String admin) {
        Disputa d = disputaRepository.findById(id).orElseThrow(() -> new IllegalStateException("Reporte no encontrado"));
        if (!"ABIERTA".equals(d.getEstado())) throw new IllegalStateException("Este reporte ya fue resuelto");
        if (decision == null || !DECISIONES.contains(decision)) throw new IllegalStateException("Elige una decisión");
        String n = nota == null ? "" : nota.trim();
        if (n.length() < 5) throw new IllegalStateException("Escribe una explicación para las dos partes");
        if (n.length() > 500) n = n.substring(0, 500);
        d.setEstado("RESUELTA");
        d.setDecision(decision);
        d.setResolucion(n);
        d.setResueltaPor(admin);
        d.setFechaResolucion(LocalDateTime.now());
        d = disputaRepository.save(d);

        try {
            Servicio s = d.getServicio();
            String texto = "Revisamos el reporte del servicio #" + s.getIdServicio() + ": " + n;
            if (s.getCliente() != null) {
                notificacionService.crear(s.getCliente().getUsuario(), "DISPUTA", texto, "/clientes/perfil", id);
            }
            if (s.getTecnico() != null) {
                notificacionService.crear(s.getTecnico().getUsuario(), "DISPUTA", texto, "/tecnicos/panel", id);
            }
        } catch (Exception ex) {
            log.warn("No se pudo avisar la resolución {}: {}", id, ex.getMessage());
        }
        return d;
    }

    @Transactional(readOnly = true)
    public List<FilaDisputa> listarFilas() {
        List<FilaDisputa> filas = new ArrayList<>();
        for (Disputa d : disputaRepository.findAllByOrderByFechaCreacionDesc()) {
            Servicio s = d.getServicio();
            var c = s.getCliente();
            var t = s.getTecnico();
            var p = s.getPago();
            filas.add(new FilaDisputa(d.getIdDisputa(), s.getIdServicio(), d.getFechaCreacion(), d.getRolAbre(),
                    d.getAbiertaPor() != null ? d.getAbiertaPor().getEmail() : "-",
                    c != null ? c.getNombres() + " " + c.getApellidoPaterno() : "-",
                    t != null ? t.getNombres() + " " + t.getApellidoPaterno() : "-",
                    s.getCategoria() != null ? s.getCategoria().getNombre() : "-",
                    s.getDescripcion(),
                    s.getMonto() != null ? "S/ " + s.getMonto() : "-",
                    p != null && p.getMetodoPago() != null ? p.getMetodoPago() : "-",
                    p != null ? p.getEstado() : "Sin pago",
                    d.getMotivo(), d.getDescripcion(), d.getEstado(), d.getDecision(), d.getResolucion(),
                    d.getFechaResolucion()));
        }
        return filas;
    }
}
