package com.hogarfix.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Límite simple de intentos de inicio de sesión (en memoria).
 * Tras {@value #MAX_INTENTOS} fallos seguidos para el mismo usuario + IP, bloquea los
 * siguientes intentos durante 15 minutos. Un login correcto reinicia el contador.
 *
 * Nota: si la app corre detrás de un proxy, la IP que ve es la del proxy, así que en la
 * práctica el límite queda por usuario. Se reinicia al reiniciar la aplicación.
 */
@Component
public class LoginAttemptService {

    static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int MAX_ENTRADAS = 10_000;

    private record Registro(int fallos, Instant primerFallo) {
    }

    private final ConcurrentHashMap<String, Registro> intentos = new ConcurrentHashMap<>();

    public static String clave(String username, String ip) {
        String usuario = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return usuario + "|" + (ip == null ? "" : ip);
    }

    public boolean estaBloqueado(String clave) {
        Registro r = intentos.get(clave);
        if (r == null) {
            return false;
        }
        if (expirado(r)) {
            intentos.remove(clave);
            return false;
        }
        return r.fallos() >= MAX_INTENTOS;
    }

    public void registrarFallo(String clave) {
        if (intentos.size() > MAX_ENTRADAS) {
            intentos.entrySet().removeIf(e -> expirado(e.getValue()));
        }
        intentos.merge(clave, new Registro(1, Instant.now()),
                (actual, nuevo) -> expirado(actual)
                        ? nuevo
                        : new Registro(actual.fallos() + 1, actual.primerFallo()));
    }

    public void registrarExito(String clave) {
        intentos.remove(clave);
    }

    private boolean expirado(Registro r) {
        return Instant.now().isAfter(r.primerFallo().plus(VENTANA));
    }
}
