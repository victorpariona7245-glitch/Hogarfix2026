package com.hogarfix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Valida números de DNI contra RENIEC usando el servicio de terceros
 * PeruAPI.com (https://peruapi.com). Requiere un API token gratuito.
 *
 * Si no hay token configurado, o la API falla/no responde, la validación
 * se salta silenciosamente (fail-open) para no bloquear el registro de
 * técnicos por una caída del servicio externo.
 */
@Service
@Slf4j
public class ReniecService {

    @Value("${reniec.api.token:}")
    private String apiToken;

    @Value("${reniec.api.url:https://peruapi.com/api/dni/}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Resultado de la validación de un DNI.
     */
    public record ResultadoValidacion(
            boolean validacionDisponible, // false si no hay token configurado o la API falló
            boolean dniValido, // true si RENIEC confirma que el DNI existe
            String nombreCompleto, // nombre completo que devuelve RENIEC (si existe)
            String mensaje // mensaje descriptivo (para logs o mostrar al usuario)
    ) {
        static ResultadoValidacion noDisponible(String motivo) {
            return new ResultadoValidacion(false, true, null, motivo);
        }
    }

    public ResultadoValidacion validarDni(String dni) {
        if (apiToken == null || apiToken.isBlank()) {
            // Validación desactivada: no hay API Key configurada todavía
            return ResultadoValidacion.noDisponible("Validación RENIEC desactivada (sin API token configurado)");
        }

        if (dni == null || !dni.matches("\\d{8}")) {
            return new ResultadoValidacion(true, false, null, "El DNI debe tener 8 dígitos numéricos");
        }

        try {
            String url = apiUrl + dni + "?api_token=" + apiToken;

            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restTemplate.getForObject(url, Map.class);

            if (respuesta == null) {
                log.warn("Respuesta vacía al validar DNI {} contra RENIEC", dni);
                return ResultadoValidacion.noDisponible("Sin respuesta del servicio de validación");
            }

            String code = String.valueOf(respuesta.get("code"));
            if ("200".equals(code)) {
                String nombreCompleto = String.valueOf(respuesta.getOrDefault("cliente", ""));
                return new ResultadoValidacion(true, true, nombreCompleto, "DNI válido");
            } else {
                String mensaje = String.valueOf(respuesta.getOrDefault("mensaje", "DNI no encontrado en RENIEC"));
                return new ResultadoValidacion(true, false, null, mensaje);
            }

        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            // El servidor respondió con un código de error (4xx/5xx) — esto suele traer
            // el motivo exacto del rechazo (token inválido, límite excedido, etc.)
            log.warn("PeruAPI.com respondió con error al validar DNI {}: status={}, body={}",
                    dni, ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResultadoValidacion.noDisponible(
                    "Error de la API (" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            // Si el servicio externo falla (caído, timeout, key inválida, etc.)
            // no bloqueamos el registro: solo se salta la validación.
            log.warn("No se pudo validar el DNI {} contra RENIEC", dni, ex);
            return ResultadoValidacion.noDisponible(
                    "No se pudo contactar el servicio de validación de DNI: "
                            + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }
    }
}