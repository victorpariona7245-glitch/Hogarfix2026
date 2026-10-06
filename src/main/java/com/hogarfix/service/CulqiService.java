package com.hogarfix.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Procesa cargos reales (en modo sandbox/integración) a través de Culqi
 * (https://culqi.com), la pasarela de pagos peruana.
 *
 * El frontend usa Culqi Checkout (Culqi.js) para tokenizar la tarjeta o
 * generar el pago por Yape directamente en el navegador del cliente — los
 * datos sensibles NUNCA llegan a este servidor, solo un "token" ya validado
 * por Culqi. Aquí solo confirmamos el cargo usando ese token.
 */
@Service
@Slf4j
public class CulqiService {

    private static final String CHARGES_URL = "https://api.culqi.com/v2/charges";

    @Value("${culqi.secret.key:}")
    private String secretKey;

    @Value("${culqi.public.key:}")
    private String publicKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean estaConfigurado() {
        return secretKey != null && !secretKey.isBlank();
    }

    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Resultado de intentar procesar un cargo con Culqi.
     */
    public record ResultadoCargo(boolean exitoso, String culqiChargeId, String mensaje) {
    }

    /**
     * Crea un cargo real (sandbox) en Culqi usando el token generado por
     * Culqi Checkout en el navegador del cliente.
     *
     * @param tokenId  el token ("tok_xxx") devuelto por Culqi Checkout
     * @param montoSol el monto a cobrar, en SOLES (se convierte a céntimos)
     * @param email    correo del cliente que paga
     * @param descripcion descripción del cargo (aparece en el panel de Culqi)
     */
    public ResultadoCargo crearCargo(String tokenId, BigDecimal montoSol, String email, String descripcion) {
        if (!estaConfigurado()) {
            return new ResultadoCargo(false, null,
                    "Culqi no está configurado todavía (falta CULQI_SECRET_KEY)");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(secretKey);

            // Culqi trabaja los montos en céntimos (S/ 120.00 -> 12000)
            long montoCentimos = montoSol.multiply(BigDecimal.valueOf(100)).longValueExact();

            Map<String, Object> body = new HashMap<>();
            body.put("amount", montoCentimos);
            body.put("currency_code", "PEN");
            body.put("email", email);
            body.put("source_id", tokenId);
            body.put("description", descripcion != null ? descripcion : "Pago de servicio HogarFix");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restTemplate
                    .exchange(CHARGES_URL, HttpMethod.POST, request, Map.class)
                    .getBody();

            if (respuesta != null && respuesta.get("id") != null) {
                String chargeId = String.valueOf(respuesta.get("id"));
                log.info("Cargo Culqi exitoso: id={}, monto={}", chargeId, montoSol);
                return new ResultadoCargo(true, chargeId, "Pago procesado correctamente");
            }

            log.warn("Respuesta inesperada de Culqi al crear cargo: {}", respuesta);
            return new ResultadoCargo(false, null, "Respuesta inesperada de la pasarela de pago");

        } catch (HttpClientErrorException ex) {
            // Culqi devuelve el motivo del rechazo en el body (fondos insuficientes,
            // tarjeta inválida, etc.)
            String mensaje = extraerMensajeError(ex.getResponseBodyAsString());
            log.warn("Cargo Culqi rechazado: {}", mensaje);
            return new ResultadoCargo(false, null, mensaje);
        } catch (Exception ex) {
            log.error("Error inesperado al procesar cargo con Culqi", ex);
            return new ResultadoCargo(false, null, "No se pudo conectar con la pasarela de pago");
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerMensajeError(String responseBody) {
        try {
            Map<String, Object> parsed = org.springframework.boot.json.JsonParserFactory.getJsonParser()
                    .parseMap(responseBody);
            Object userMessage = parsed.get("user_message");
            if (userMessage != null) return String.valueOf(userMessage);
            Object merchantMessage = parsed.get("merchant_message");
            if (merchantMessage != null) return String.valueOf(merchantMessage);
        } catch (Exception ignored) {
            // si no se puede parsear, devolvemos un mensaje genérico
        }
        return "El pago fue rechazado por la pasarela de pago";
    }
}