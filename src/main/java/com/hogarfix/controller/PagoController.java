package com.hogarfix.controller;

import com.hogarfix.model.Pago;
import com.hogarfix.dto.PagoDTO;
import com.hogarfix.mapper.PagoMapper;
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