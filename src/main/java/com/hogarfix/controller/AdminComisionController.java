package com.hogarfix.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hogarfix.model.Pago;
import com.hogarfix.service.AuditoriaService;
import com.hogarfix.service.PagoService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Panel del admin: cobros, comisiones, revisión de pagos Yape/Plin y liquidación a técnicos. */
@Controller
@RequiredArgsConstructor
@org.springframework.web.bind.annotation.RequestMapping("/admin")
public class AdminComisionController {

    private final PagoService pagoService;
    private final AuditoriaService auditoriaService;

    @Getter
    @AllArgsConstructor
    public static class FilaCobro {
        private Pago pago;
        private String cliente;
        private String tecnico;
        private BigDecimal comision;
        private BigDecimal neto;
        private boolean efectivo;
        private boolean liquidado;
    }

    @GetMapping("/comisiones")
    public String ver(Model model, Principal principal) {
        if (principal == null) return "redirect:/auth/login";

        List<Pago> pagos = new ArrayList<>(pagoService.listarPagosParaExportar());
        pagos.sort(Comparator.comparing(Pago::getIdPago).reversed());

        List<FilaCobro> filas = new ArrayList<>();
        List<FilaCobro> enRevision = new ArrayList<>();
        BigDecimal totalCobrado = BigDecimal.ZERO;
        BigDecimal totalComision = BigDecimal.ZERO;
        BigDecimal porTransferir = BigDecimal.ZERO;
        BigDecimal porCobrarEfectivo = BigDecimal.ZERO;

        for (Pago p : pagos) {
            BigDecimal monto = p.getMonto() == null ? BigDecimal.ZERO : p.getMonto();
            BigDecimal comision = p.getComision() != null ? p.getComision() : pagoService.calcularComision(monto);
            BigDecimal neto = p.getMontoTecnico() != null ? p.getMontoTecnico() : monto.subtract(comision);
            boolean efectivo = "efectivo".equalsIgnoreCase(p.getMetodoPago());
            boolean liquidado = Boolean.TRUE.equals(p.getLiquidado());

            String cliente = p.getCliente() != null
                    ? p.getCliente().getNombres() + " " + p.getCliente().getApellidoPaterno() : "N/A";
            String tecnico = (p.getServicio() != null && p.getServicio().getTecnico() != null)
                    ? p.getServicio().getTecnico().getNombres() + " "
                            + p.getServicio().getTecnico().getApellidoPaterno()
                    : "Sin asignar";

            FilaCobro fila = new FilaCobro(p, cliente, tecnico, comision, neto, efectivo, liquidado);
            filas.add(fila);

            if ("EN_REVISION".equalsIgnoreCase(p.getEstado())) {
                enRevision.add(fila);
            }
            if ("PAGADO".equalsIgnoreCase(p.getEstado())) {
                totalCobrado = totalCobrado.add(monto);
                totalComision = totalComision.add(comision);
                if (!liquidado) {
                    if (efectivo) porCobrarEfectivo = porCobrarEfectivo.add(comision);
                    else porTransferir = porTransferir.add(neto);
                }
            }
        }

        model.addAttribute("filas", filas);
        model.addAttribute("enRevision", enRevision);
        model.addAttribute("totalCobrado", totalCobrado);
        model.addAttribute("totalComision", totalComision);
        model.addAttribute("porTransferir", porTransferir);
        model.addAttribute("porCobrarEfectivo", porCobrarEfectivo);
        model.addAttribute("porcentaje", pagoService.getPorcentajeComision());
        return "admin/comisiones";
    }

    @PostMapping("/comisiones/{id}/aprobar")
    public String aprobar(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/auth/login";
        try {
            Pago p = pagoService.aprobarManual(id);
            auditoriaService.registrar(principal.getName(), "Aprobó pago",
                    "Pago #" + id + " (" + p.getMetodoPago() + ", S/ " + p.getMonto() + ", operación "
                            + p.getCodigoOperacion() + ")");
            ra.addFlashAttribute("mensajeExito", "Pago #" + id + " aprobado.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/comisiones";
    }

    @PostMapping("/comisiones/{id}/rechazar")
    public String rechazar(@PathVariable Long id, @RequestParam(value = "motivo", required = false) String motivo,
            Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/auth/login";
        try {
            pagoService.rechazarManual(id, motivo);
            auditoriaService.registrar(principal.getName(), "Rechazó pago",
                    "Pago #" + id + (motivo == null || motivo.isBlank() ? "" : " — " + motivo));
            ra.addFlashAttribute("mensajeExito", "Pago #" + id + " devuelto al cliente como pendiente.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/comisiones";
    }

    @PostMapping("/comisiones/{id}/liquidar")
    public String liquidar(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/auth/login";
        try {
            Pago p = pagoService.marcarLiquidado(id);
            auditoriaService.registrar(principal.getName(), "Liquidó pago",
                    "Pago #" + id + " (" + p.getMetodoPago() + ")");
            ra.addFlashAttribute("mensajeExito", "Pago #" + id + " marcado como liquidado.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/comisiones";
    }
}
