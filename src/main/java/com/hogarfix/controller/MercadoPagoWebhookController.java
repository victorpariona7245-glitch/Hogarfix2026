package com.hogarfix.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hogarfix.service.MercadoPagoService;
import com.hogarfix.service.PagoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Aviso automático de MercadoPago cuando cambia el estado de un pago. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final MercadoPagoService mercadoPagoService;
    private final PagoService pagoService;

    @PostMapping("/pagos/mp-webhook")
    public ResponseEntity<String> recibir(@RequestParam Map<String, String> params,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String tipo = params.getOrDefault("type", params.get("topic"));
            if (tipo == null && body != null && body.get("type") != null) {
                tipo = String.valueOf(body.get("type"));
            }
            if (tipo == null || !tipo.equalsIgnoreCase("payment")) {
                return ResponseEntity.ok("ignorado");
            }

            String paymentId = params.get("data.id");
            if (paymentId == null) paymentId = params.get("id");
            if (paymentId == null && body != null && body.get("data") instanceof Map<?, ?> data
                    && data.get("id") != null) {
                paymentId = String.valueOf(data.get("id"));
            }
            if (paymentId == null) {
                return ResponseEntity.ok("sin id");
            }

            // Nunca se confía en el aviso: se consulta el pago real a MercadoPago
            var v = mercadoPagoService.consultarPago(paymentId);
            if (!v.exitoso() || !v.aprobado() || v.externalReference() == null) {
                return ResponseEntity.ok("no aprobado");
            }

            Long pagoId;
            try {
                pagoId = Long.valueOf(v.externalReference());
            } catch (NumberFormatException ex) {
                return ResponseEntity.ok("referencia invalida");
            }

            var pagoOpt = pagoService.buscarPorIdConDetalle(pagoId);
            if (pagoOpt.isEmpty()) {
                return ResponseEntity.ok("pago inexistente");
            }
            var pago = pagoOpt.get();
            boolean coincide = v.monto() != null && pago.getMonto() != null
                    && pago.getMonto().compareTo(v.monto()) == 0
                    && "PEN".equalsIgnoreCase(v.moneda());
            if (!coincide) {
                log.warn("Webhook MercadoPago: el pago {} no coincide con el cobro {}", paymentId, pagoId);
                return ResponseEntity.ok("no coincide");
            }

            if (!"PAGADO".equalsIgnoreCase(pago.getEstado())) {
                pagoService.marcarPagadoConReferencia(pagoId, "mercadopago", paymentId);
                log.info("Pago {} confirmado por webhook de MercadoPago (payment {})", pagoId, paymentId);
            }
        } catch (Exception ex) {
            log.warn("Error procesando webhook de MercadoPago: {}", ex.getMessage());
        }
        // Siempre 200 para que MercadoPago no reintente indefinidamente
        return ResponseEntity.ok("ok");
    }
}
