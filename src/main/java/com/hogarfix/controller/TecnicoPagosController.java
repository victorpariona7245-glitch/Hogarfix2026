package com.hogarfix.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hogarfix.model.Pago;
import com.hogarfix.model.Tecnico;
import com.hogarfix.repository.PagoRepository;
import com.hogarfix.service.PagoService;
import com.hogarfix.service.TecnicoService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** "Mis pagos": el técnico ve cuánto cobró en cada servicio, la comisión descontada y lo que le queda. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/tecnicos")
public class TecnicoPagosController {

    private final PagoRepository pagoRepository;
    private final PagoService pagoService;
    private final TecnicoService tecnicoService;

    @Getter
    @AllArgsConstructor
    public static class FilaPago {
        private LocalDateTime fecha;
        private String servicio;
        private String cliente;
        private String metodo;
        private boolean efectivo;
        private BigDecimal monto;
        private BigDecimal comision;
        private BigDecimal neto;
        private String estado; // EN_REVISION, PENDIENTE_LIQ, LIQUIDADO
    }

    @GetMapping("/pagos")
    @Transactional(readOnly = true)
    public String verPagos(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth/login";

        Tecnico tecnico;
        try {
            tecnico = tecnicoService.obtenerPorEmail(principal.getName());
        } catch (Exception ex) {
            return "redirect:/auth/login";
        }

        List<FilaPago> filas = new ArrayList<>();
        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalComision = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal porRecibir = BigDecimal.ZERO; // Yape/Plin: HogarFix aún no te lo transfiere
        BigDecimal porEntregar = BigDecimal.ZERO; // Efectivo: comisión que aún debes entregar

        for (Pago p : pagoRepository.findPagosDeTecnico(tecnico.getIdTecnico())) {
            BigDecimal monto = p.getMonto() == null ? BigDecimal.ZERO : p.getMonto();
            BigDecimal comision = p.getComision() != null ? p.getComision() : pagoService.calcularComision(monto);
            BigDecimal neto = p.getMontoTecnico() != null ? p.getMontoTecnico() : monto.subtract(comision);
            boolean efectivo = "efectivo".equalsIgnoreCase(p.getMetodoPago());
            boolean liquidado = Boolean.TRUE.equals(p.getLiquidado());

            String estado;
            if ("EN_REVISION".equalsIgnoreCase(p.getEstado())) {
                estado = "EN_REVISION";
            } else if (liquidado) {
                estado = "LIQUIDADO";
            } else {
                estado = "PENDIENTE_LIQ";
                if (efectivo) porEntregar = porEntregar.add(comision);
                else porRecibir = porRecibir.add(neto);
            }

            if ("PAGADO".equalsIgnoreCase(p.getEstado())) {
                totalCobrado = totalCobrado.add(monto);
                totalComision = totalComision.add(comision);
                totalNeto = totalNeto.add(neto);
            }

            String cliente = p.getCliente() != null
                    ? p.getCliente().getNombres() + " " + p.getCliente().getApellidoPaterno() : "Cliente";
            String servicio = (p.getServicio() != null && p.getServicio().getCategoria() != null)
                    ? p.getServicio().getCategoria().getNombre() : "Servicio";

            filas.add(new FilaPago(p.getFechaPago(), servicio, cliente,
                    p.getMetodoPago() == null ? "-" : p.getMetodoPago(), efectivo, monto, comision, neto, estado));
        }

        model.addAttribute("tecnico", tecnico);
        model.addAttribute("filas", filas);
        model.addAttribute("totalCobrado", totalCobrado);
        model.addAttribute("totalComision", totalComision);
        model.addAttribute("totalNeto", totalNeto);
        model.addAttribute("porRecibir", porRecibir);
        model.addAttribute("porEntregar", porEntregar);
        model.addAttribute("porcentaje", pagoService.getPorcentajeComision());
        return "tecnico/pagos";
    }
}
