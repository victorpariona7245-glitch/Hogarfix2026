package com.hogarfix.controller;

import com.hogarfix.model.Pago;
import com.hogarfix.dto.PagoDTO;
import com.hogarfix.mapper.PagoMapper;
import com.hogarfix.service.CulqiService;
import com.hogarfix.service.PagoService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final CulqiService culqiService;

    @GetMapping
    public ResponseEntity<List<PagoDTO>> listarPagos() {
        var pagos = pagoService.listarPagos();
        var dtos = pagos.stream().map(PagoMapper::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<Pago> registrar(@RequestBody Pago pago) {
        return ResponseEntity.ok(pagoService.registrarPago(pago));
    }

    /**
     * Devuelve si Culqi está configurado y, de ser así, la llave PÚBLICA
     * (segura de exponer al navegador) para inicializar el widget de pago.
     */
    @GetMapping("/culqi-config")
    public ResponseEntity<Map<String, Object>> culqiConfig() {
        return ResponseEntity.ok(Map.of(
                "configurado", culqiService.estaConfigurado(),
                "publicKey", culqiService.estaConfigurado() ? culqiService.getPublicKey() : ""));
    }

    /**
     * Procesa un cargo REAL (modo sandbox) con Culqi usando el token que el
     * navegador del cliente generó a través de Culqi Checkout.
     */
    @PostMapping("/{id}/procesar-culqi")
    public ResponseEntity<Map<String, Object>> procesarConCulqi(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {

        String tokenId = body.get("tokenId");
        if (tokenId == null || tokenId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", "Falta el token de pago"));
        }

        var pagoOpt = pagoService.buscarPorIdConDetalle(id);
        if (pagoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var pago = pagoOpt.get();

        if ("PAGADO".equalsIgnoreCase(pago.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", "Este pago ya fue procesado"));
        }

        String email = (pago.getCliente() != null && pago.getCliente().getUsuario() != null)
                ? pago.getCliente().getUsuario().getEmail()
                : "cliente@hogarfix.com";
        String descripcion = "Servicio HogarFix" + (pago.getServicio() != null && pago.getServicio().getCategoria() != null
                ? " - " + pago.getServicio().getCategoria().getNombre()
                : "");

        var resultado = culqiService.crearCargo(tokenId, pago.getMonto(), email, descripcion);

        if (!resultado.exitoso()) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "mensaje", resultado.mensaje()));
        }

        var actualizado = pagoService.marcarPagadoConReferencia(id, "culqi", resultado.culqiChargeId());
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "pagoId", actualizado.map(Pago::getIdPago).orElse(id),
                "referencia", resultado.culqiChargeId()));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable("id") Long id) {
        var opt = pagoService.marcarPagado(id);
        if (opt.isPresent()) {
            // Return a small JSON payload to avoid serializing the full entity (Hibernate proxies can break Jackson)
            return ResponseEntity.ok(Map.of("status", "OK", "pagoId", opt.get().getIdPago()));
        }
        return ResponseEntity.notFound().build();
    }
}