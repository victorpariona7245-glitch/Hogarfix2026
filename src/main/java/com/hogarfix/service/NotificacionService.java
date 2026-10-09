package com.hogarfix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.Notificacion;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.NotificacionRepository;
import com.hogarfix.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    /** Crea un aviso. Nunca lanza excepción: una notificación no debe romper el flujo principal. */
    public void crear(Usuario destino, String tipo, String mensaje, String enlace, Long idReferencia) {
        if (destino == null) return;
        try {
            if (mensaje != null && mensaje.length() > 300) {
                mensaje = mensaje.substring(0, 297) + "...";
            }
            notificacionRepository.save(Notificacion.builder()
                    .usuario(destino)
                    .tipo(tipo)
                    .mensaje(mensaje)
                    .enlace(enlace)
                    .idReferencia(idReferencia)
                    .leida(false)
                    .fecha(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.warn("No se pudo crear la notificación: {}", ex.getMessage());
        }
    }

    /** Igual que crear, pero evita acumular avisos repetidos mientras el anterior siga sin leer. */
    public void crearSiNoHayPendiente(Usuario destino, String tipo, String mensaje, String enlace, Long idReferencia) {
        if (destino == null) return;
        try {
            if (notificacionRepository.existsByUsuarioAndTipoAndIdReferenciaAndLeidaFalse(destino, tipo, idReferencia)) {
                return;
            }
        } catch (Exception ex) {
            log.warn("No se pudo comprobar notificaciones pendientes: {}", ex.getMessage());
        }
        crear(destino, tipo, mensaje, enlace, idReferencia);
    }

    public Optional<Usuario> usuarioPorLogin(String login) {
        if (login == null) return Optional.empty();
        return usuarioRepository.findByEmail(login).or(() -> usuarioRepository.findByUsername(login));
    }

    @Transactional(readOnly = true)
    public List<Notificacion> ultimas(Usuario usuario) {
        return notificacionRepository.findTop15ByUsuarioOrderByFechaDesc(usuario);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }

    @Transactional
    public void marcarLeida(Long id, Usuario usuario) {
        notificacionRepository.findById(id).ifPresent(n -> {
            if (n.getUsuario() != null && n.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        });
    }

    @Transactional
    public void marcarTodasLeidas(Usuario usuario) {
        notificacionRepository.marcarTodasLeidas(usuario);
    }

    /** Limpieza diaria: borra avisos de más de 60 días. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void limpiarAntiguas() {
        int n = notificacionRepository.borrarAnterioresA(LocalDateTime.now().minusDays(60));
        if (n > 0) log.info("Notificaciones antiguas eliminadas: {}", n);
    }
}
