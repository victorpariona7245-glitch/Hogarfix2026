package com.hogarfix.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.hogarfix.service.MercadoPagoService;
import com.hogarfix.service.PagoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;
    private final PagoService pagoService;

    // Usado para construir las back_urls (a dónde vuelve el cliente tras pagar).
    // En desarrollo local suele ser http://localhost:8080
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Crea la preferencia de pago en MercadoPago y redirige al cliente a la
     * página de pago hospedada por MercadoPago (Checkout Pro).
     */
    @GetMapping("/pagos/{id}/mp-iniciar")
    public RedirectView iniciarPago(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        var pagoOpt = pagoService.buscarPorIdConDetalle(id);
        if (pagoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Pago no encontrado");
            return new RedirectView("/pagos?pagoId=" + id);
        }
        var pago = pagoOpt.get();

        if ("PAGADO".equalsIgnoreCase(pago.getEstado())) {
            redirectAttributes.addFlashAttribute("mensajeError", "Este pago ya fue procesado");
            return new RedirectView("/pagos?pagoId=" + id);
        }

        String email = (pago.getCliente() != null && pago.getCliente().getUsuario() != null)
                ? pago.getCliente().getUsuario().getEmail()
                : null;
        String descripcion = "Servicio HogarFix"
                + (pago.getServicio() != null && pago.getServicio().getCategoria() != null
                        ? " - " + pago.getServicio().getCategoria().getNombre()
                        : "");

        String successUrl = baseUrl + "/pagos/mp-retorno?resultado=success";
        String failureUrl = baseUrl + "/pagos/mp-retorno?resultado=failure";
        String pendingUrl = baseUrl + "/pagos/mp-retorno?resultado=pending";

        var resultado = mercadoPagoService.crearPreferencia(
                id, pago.getMonto(), descripcion, email, successUrl, failureUrl, pendingUrl);

        if (!resultado.exitoso()) {
            log.warn("No se pudo crear preferencia MercadoPago para pago {}: {}", id, resultado.mensaje());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se pudo iniciar el pago: " + resultado.mensaje());
            return new RedirectView("/pagos?pagoId=" + id);
        }

        return new RedirectView(resultado.initPoint());
    }

    /**
     * MercadoPago redirige aquí al cliente después de completar (o cancelar)
     * el pago. Verificamos el estado REAL consultando la API de MercadoPago
     * (nunca confiar solo en los parámetros de la URL).
     */
    @GetMapping("/pagos/mp-retorno")
    public RedirectView retornoPago(
            @RequestParam(value = "payment_id", required = false) String paymentId,
            @RequestParam(value = "external_reference", required = false) String externalReference,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "resultado", required = false) String resultado,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (paymentId == null || externalReference == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo confirmar el resultado del pago");
            return new RedirectView("/");
        }

        Long pagoId;
        try {
            pagoId = Long.valueOf(externalReference);
        } catch (NumberFormatException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "Referencia de pago inválida");
            return new RedirectView("/");
        }

        var verificacion = mercadoPagoService.consultarPago(paymentId);

        if (verificacion.exitoso() && verificacion.aprobado()) {
            pagoService.marcarPagadoConReferencia(pagoId, "mercadopago", paymentId);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Pago realizado con éxito!");
        } else {
            String estadoTexto = verificacion.status() != null ? verificacion.status() : "desconocido";
            log.warn("Pago {} no aprobado, estado MercadoPago: {}", pagoId, estadoTexto);
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El pago no se completó (estado: " + estadoTexto + "). Puedes intentarlo de nuevo.");
        }

        return new RedirectView("/");
    }
}