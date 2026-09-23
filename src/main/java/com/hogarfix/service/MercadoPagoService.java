package com.hogarfix.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Integración con MercadoPago Checkout Pro (modo sandbox/pruebas).
 *
 * A diferencia de Culqi Custom Checkout (widget embebido), Checkout Pro
 * redirige al cliente a una página de pago hospedada por MercadoPago —
 * ahí ingresa sus datos de forma 100% segura, y luego MercadoPago lo
 * redirige de vuelta a nuestra aplicación con el resultado. Esto simplifica
 * mucho la integración: no manejamos ningún dato de tarjeta.
 */
@Service
@Slf4j
public class MercadoPagoService {

    private static final String PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";
    private static final String PAYMENTS_URL = "https://api.mercadopago.com/v1/payments/";

    @Value("${mercadopago.access.token:}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean estaConfigurado() {
        return accessToken != null && !accessToken.isBlank();
    }

    public record ResultadoPreferencia(boolean exitoso, String initPoint, String preferenceId, String mensaje) {
    }

    public record ResultadoPago(boolean exitoso, boolean aprobado, String status, String mensaje) {
    }

    /**
     * Crea una "preferencia" de pago en MercadoPago y devuelve la URL a la que
     * hay que redirigir al cliente para que complete el pago.
     */
    public ResultadoPreferencia crearPreferencia(Long pagoId, BigDecimal montoSol, String descripcion,
            String emailComprador, String successUrl, String failureUrl, String pendingUrl) {

        if (!estaConfigurado()) {
            return new ResultadoPreferencia(false, null, null,
                    "MercadoPago no está configurado todavía (falta MERCADOPAGO_ACCESS_TOKEN)");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> item = new HashMap<>();
            item.put("title", descripcion != null ? descripcion : "Servicio HogarFix");
            item.put("quantity", 1);
            item.put("unit_price", montoSol.doubleValue());
            item.put("currency_id", "PEN");

            Map<String, Object> payer = new HashMap<>();
            if (emailComprador != null && !emailComprador.isBlank()) {
                payer.put("email", emailComprador);
            }

            Map<String, Object> backUrls = new HashMap<>();
            backUrls.put("success", successUrl);
            backUrls.put("failure", failureUrl);
            backUrls.put("pending", pendingUrl);

            Map<String, Object> body = new HashMap<>();
            body.put("items", List.of(item));
            if (!payer.isEmpty()) body.put("payer", payer);
            body.put("back_urls", backUrls);
            body.put("auto_return", "approved");
            body.put("external_reference", String.valueOf(pagoId));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restTemplate
                    .exchange(PREFERENCES_URL, HttpMethod.POST, request, Map.class)
                    .getBody();

            if (respuesta != null && respuesta.get("init_point") != null) {
                String initPoint = String.valueOf(respuesta.get("init_point"));
                String preferenceId = String.valueOf(respuesta.get("id"));
                log.info("Preferencia MercadoPago creada: id={}, pagoId={}", preferenceId, pagoId);
                return new ResultadoPreferencia(true, initPoint, preferenceId, "OK");
            }

            log.warn("Respuesta inesperada de MercadoPago al crear preferencia: {}", respuesta);
            return new ResultadoPreferencia(false, null, null, "Respuesta inesperada de MercadoPago");

        } catch (Exception ex) {
            log.error("Error al crear preferencia en MercadoPago", ex);
            return new ResultadoPreferencia(false, null, null, "No se pudo conectar con MercadoPago");
        }
    }

    /**
     * Verifica el estado real de un pago consultando directamente la API de
     * MercadoPago (nunca confiar solo en los parámetros de la URL de retorno,
     * ya que podrían ser manipulados).
     */
    public ResultadoPago consultarPago(String paymentId) {
        if (!estaConfigurado()) {
            return new ResultadoPago(false, false, null, "MercadoPago no está configurado");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restTemplate
                    .exchange(PAYMENTS_URL + paymentId, HttpMethod.GET, request, Map.class)
                    .getBody();

            if (respuesta == null) {
                return new ResultadoPago(false, false, null, "Sin respuesta de MercadoPago");
            }

            String status = String.valueOf(respuesta.get("status")); // approved, pending, rejected, etc.
            boolean aprobado = "approved".equalsIgnoreCase(status);
            return new ResultadoPago(true, aprobado, status, "OK");

        } catch (Exception ex) {
            log.error("Error al consultar pago {} en MercadoPago", paymentId, ex);
            return new ResultadoPago(false, false, null, "No se pudo verificar el pago con MercadoPago");
        }
    }
}