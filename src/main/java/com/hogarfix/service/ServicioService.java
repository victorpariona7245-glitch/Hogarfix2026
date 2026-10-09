package com.hogarfix.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.Servicio;
import com.hogarfix.model.ServicioHistorial;
import com.hogarfix.repository.ServicioHistorialRepository;
import com.hogarfix.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lógica de negocio de las solicitudes de servicio.
 *
 * Flujo de estados:
 * PENDIENTE → ACEPTADO → EN_CAMINO → EN_PROCESO → FINALIZADO
 * y CANCELADO desde PENDIENTE, ACEPTADO o EN_CAMINO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioService {

    public static final String ACTOR_CLIENTE = "CLIENTE";
    public static final String ACTOR_TECNICO = "TECNICO";
    public static final String ACTOR_SISTEMA = "SISTEMA";

    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "PENDIENTE", Set.of("ACEPTADO", "EN_PROCESO", "CANCELADO"),
            "ACEPTADO", Set.of("EN_CAMINO", "EN_PROCESO", "CANCELADO"),
            "EN_CAMINO", Set.of("EN_PROCESO", "CANCELADO"),
            "EN_PROCESO", Set.of("FINALIZADO"),
            "FINALIZADO", Set.of(),
            "CANCELADO", Set.of());

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicioRepository servicioRepository;
    private final ServicioHistorialRepository historialRepository;
    private final NotificacionService notificacionService;

    public Servicio registrarServicio(Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    /** Guarda una solicitud nueva, deja constancia en el historial y avisa al técnico. */
    @Transactional
    public Servicio registrarSolicitudNueva(Servicio servicio) {
        Servicio s = servicioRepository.save(servicio);
        guardarHistorial(s, "PENDIENTE", ACTOR_CLIENTE, "Solicitud creada");
        try {
            if (s.getTecnico() != null && s.getTecnico().getUsuario() != null) {
                String cat = s.getCategoria() != null ? s.getCategoria().getNombre() : "servicio";
                String quien = s.getCliente() != null ? s.getCliente().getNombres() : "Un cliente";
                notificacionService.crear(s.getTecnico().getUsuario(), "SOLICITUD",
                        "Nueva solicitud de " + cat + " de " + quien, "/tecnicos/panel", s.getIdServicio());
            }
        } catch (Exception ex) {
            log.warn("No se pudo notificar la nueva solicitud: {}", ex.getMessage());
        }
        return s;
    }

    public List<Servicio> listarServicios() {
        return servicioRepository.findAll();
    }

    public List<Servicio> listarPorCliente(com.hogarfix.model.Cliente cliente) {
        return servicioRepository.findByCliente(cliente);
    }

    public Optional<Servicio> buscarPorId(Long id) {
        return servicioRepository.findById(id);
    }

    // --- Atajos por estado ---

    public Optional<Servicio> marcarAceptado(Long id) {
        return cambiarEstado(id, "ACEPTADO", ACTOR_TECNICO, "El técnico aceptó la solicitud");
    }

    public Optional<Servicio> marcarEnCamino(Long id) {
        return cambiarEstado(id, "EN_CAMINO", ACTOR_TECNICO, "El técnico va en camino");
    }

    public Optional<Servicio> marcarEnProgreso(Long id) {
        return cambiarEstado(id, "EN_PROCESO", ACTOR_TECNICO, "El técnico inició el servicio");
    }

    public Optional<Servicio> marcarFinalizado(Long id) {
        return cambiarEstado(id, "FINALIZADO", ACTOR_TECNICO, "Servicio finalizado");
    }

    public Optional<Servicio> marcarCancelado(Long id) {
        return cambiarEstado(id, "CANCELADO", ACTOR_TECNICO, "Cancelado por el técnico");
    }

    /**
     * Cambia el estado validando la transición, guarda el historial y avisa a la
     * otra parte. Lanza IllegalStateException si la transición no está permitida.
     */
    @Transactional
    public Optional<Servicio> cambiarEstado(Long id, String nuevo, String actor, String detalle) {
        var opt = servicioRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Servicio s = opt.get();
        String actual = s.getEstado() == null ? "" : s.getEstado().toUpperCase();
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new IllegalStateException("Transición no permitida: " + actual + " → " + nuevo);
        }
        s.setEstado(nuevo);
        if ("FINALIZADO".equals(nuevo)) {
            s.setFechaFinalizacion(LocalDateTime.now());
        }
        s = servicioRepository.save(s);
        guardarHistorial(s, nuevo, actor, detalle);
        notificarCambio(s, nuevo, actor);
        return Optional.of(s);
    }

    private void guardarHistorial(Servicio s, String estado, String actor, String detalle) {
        try {
            historialRepository.save(ServicioHistorial.builder()
                    .servicio(s).estado(estado).actor(actor).detalle(detalle)
                    .fecha(LocalDateTime.now()).build());
        } catch (Exception ex) {
            log.warn("No se pudo guardar el historial del servicio {}: {}", s.getIdServicio(), ex.getMessage());
        }
    }

    private void notificarCambio(Servicio s, String nuevo, String actor) {
        try {
            String cat = s.getCategoria() != null ? s.getCategoria().getNombre() : "servicio";
            String tecnico = s.getTecnico() != null ? s.getTecnico().getNombres() : "El técnico";
            var usuCliente = s.getCliente() != null ? s.getCliente().getUsuario() : null;
            var usuTecnico = s.getTecnico() != null ? s.getTecnico().getUsuario() : null;
            Long ref = s.getIdServicio();
            String linkCliente = "/clientes/perfil";
            String linkTecnico = "/tecnicos/panel";

            switch (nuevo) {
                case "ACEPTADO" -> notificacionService.crear(usuCliente, "ESTADO",
                        tecnico + " aceptó tu solicitud de " + cat, linkCliente, ref);
                case "EN_CAMINO" -> notificacionService.crear(usuCliente, "ESTADO",
                        tecnico + " va en camino (" + cat + ")", linkCliente, ref);
                case "EN_PROCESO" -> notificacionService.crear(usuCliente, "ESTADO",
                        tecnico + " comenzó el servicio de " + cat, linkCliente, ref);
                case "FINALIZADO" -> notificacionService.crear(usuCliente, "ESTADO",
                        "Tu servicio de " + cat + " finalizó. Ya puedes pagar y calificar", linkCliente, ref);
                case "CANCELADO" -> {
                    if (ACTOR_CLIENTE.equals(actor)) {
                        notificacionService.crear(usuTecnico, "ESTADO",
                                "El cliente canceló la solicitud de " + cat, linkTecnico, ref);
                    } else if (ACTOR_SISTEMA.equals(actor)) {
                        notificacionService.crear(usuCliente, "ESTADO",
                                "Tu solicitud de " + cat + " venció sin respuesta del técnico", linkCliente, ref);
                    } else {
                        notificacionService.crear(usuCliente, "ESTADO",
                                tecnico + " canceló tu solicitud de " + cat, linkCliente, ref);
                    }
                }
                default -> { }
            }
        } catch (Exception ex) {
            log.warn("No se pudo notificar el cambio de estado: {}", ex.getMessage());
        }
    }

    /** Historial listo para mostrar (si no hay registros antiguos, se arma con la fecha de solicitud). */
    @Transactional(readOnly = true)
    public List<Map<String, String>> historial(Long idServicio) {
        List<Map<String, String>> out = new ArrayList<>();
        for (ServicioHistorial h : historialRepository.findByServicio_IdServicioOrderByFechaAsc(idServicio)) {
            out.add(Map.of(
                    "estado", h.getEstado(),
                    "actor", h.getActor() == null ? "" : h.getActor(),
                    "detalle", h.getDetalle() == null ? "" : h.getDetalle(),
                    "fecha", h.getFecha().format(FORMATO)));
        }
        if (out.isEmpty()) {
            servicioRepository.findById(idServicio).ifPresent(s -> out.add(Map.of(
                    "estado", s.getEstado() == null ? "" : s.getEstado(),
                    "actor", "",
                    "detalle", "Estado actual",
                    "fecha", s.getFechaSolicitud() != null ? s.getFechaSolicitud().format(FORMATO) : "")));
        }
        return out;
    }

    public void eliminarServicio(Long id) {
        servicioRepository.deleteById(id);
    }

    public List<Servicio> obtenerPorTecnico(Long idTecnico) {
        return servicioRepository.findByTecnico_IdTecnico(idTecnico);
    }
}
