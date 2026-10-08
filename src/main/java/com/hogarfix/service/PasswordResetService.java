package com.hogarfix.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.PasswordResetToken;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.ClienteRepository;
import com.hogarfix.repository.PasswordResetTokenRepository;
import com.hogarfix.repository.TecnicoRepository;
import com.hogarfix.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recuperación de contraseña por correo:
 * - El enlace dura 30 minutos y solo sirve una vez.
 * - En la base de datos solo se guarda el hash del token.
 * - Si el correo no existe no se revela (la pantalla responde igual).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final Duration VIGENCIA = Duration.ofMinutes(30);
    private static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofMinutes(1);
    /** Tope global de correos de recuperación por hora, para no agotar el límite diario de Brevo. */
    private static final int MAX_ENVIOS_POR_HORA = 60;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final SecureRandom random = new SecureRandom();
    private final Deque<Instant> enviosRecientes = new ArrayDeque<>();

    /** Genera el enlace y lo envía por correo. No lanza error ni dice si el correo existe. */
    @Transactional
    public void solicitar(String email) {
        if (email == null || email.isBlank() || email.length() > 100) {
            return;
        }
        Optional<Usuario> opt = usuarioRepository.findByEmail(email.trim());
        if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getIsActivo())) {
            return;
        }
        Usuario usuario = opt.get();

        if (tokenRepository.existsByUsuarioAndCreadoEnAfter(usuario, LocalDateTime.now().minus(ESPERA_ENTRE_ENVIOS))) {
            return;
        }
        if (!permitirEnvio()) {
            log.warn("Se superó el tope de correos de recuperación por hora");
            return;
        }

        tokenRepository.borrarDeUsuario(usuario);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken entidad = new PasswordResetToken();
        entidad.setTokenHash(hash(token));
        entidad.setUsuario(usuario);
        entidad.setCreadoEn(LocalDateTime.now());
        entidad.setExpiraEn(LocalDateTime.now().plus(VIGENCIA));
        tokenRepository.save(entidad);

        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String enlace = base + "/auth/restablecer?token=" + token;
        emailService.sendRecuperarPasswordEmail(usuario.getEmail(), nombreDe(usuario), enlace);
    }

    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return buscarValido(token).isPresent();
    }

    /** Cambia la contraseña si el token es válido. Devuelve false si no lo es. */
    @Transactional
    public boolean restablecer(String token, String nuevaPassword) {
        Optional<PasswordResetToken> opt = buscarValido(token);
        if (opt.isEmpty()) {
            return false;
        }
        Usuario usuario = opt.get().getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        tokenRepository.borrarDeUsuario(usuario); // el enlace deja de servir
        log.info("Contraseña restablecida para usuarioId={}", usuario.getIdUsuario());
        return true;
    }

    /** Limpieza diaria de enlaces vencidos. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpiarVencidos() {
        int borrados = tokenRepository.borrarExpirados(LocalDateTime.now());
        if (borrados > 0) {
            log.info("Enlaces de recuperación vencidos eliminados: {}", borrados);
        }
    }

    private Optional<PasswordResetToken> buscarValido(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            return Optional.empty();
        }
        return tokenRepository.findByTokenHash(hash(token.trim()))
                .filter(t -> t.getExpiraEn().isAfter(LocalDateTime.now()));
    }

    private String nombreDe(Usuario usuario) {
        String nombre = clienteRepository.findByUsuario_Email(usuario.getEmail())
                .map(c -> c.getNombres())
                .orElseGet(() -> tecnicoRepository.findByUsuario_Email(usuario.getEmail())
                        .map(t -> t.getNombres())
                        .orElse(""));
        return nombre == null ? "" : nombre;
    }

    private synchronized boolean permitirEnvio() {
        Instant ahora = Instant.now();
        Instant limite = ahora.minus(Duration.ofHours(1));
        while (!enviosRecientes.isEmpty() && enviosRecientes.peekFirst().isBefore(limite)) {
            enviosRecientes.pollFirst();
        }
        if (enviosRecientes.size() >= MAX_ENVIOS_POR_HORA) {
            return false;
        }
        enviosRecientes.addLast(ahora);
        return true;
    }

    private static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
