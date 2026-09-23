package com.hogarfix.controller;

import com.hogarfix.model.Cliente;
import com.hogarfix.model.Pago;
import com.hogarfix.service.ClienteService;
import com.hogarfix.service.MercadoPagoService;
import com.hogarfix.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class VistaPagoController {

    private final PagoService pagoService;
    private final ClienteService clienteService;
    private final MercadoPagoService mercadoPagoService;
    private static final Logger logger = LoggerFactory.getLogger(VistaPagoController.class);

    @GetMapping("/pagos")
    public String verPagos(Principal principal, HttpSession session, Model model, @org.springframework.web.bind.annotation.RequestParam(value = "pagoId", required = false) Long pagoId, jakarta.servlet.http.HttpServletRequest request) {
        Cliente cliente = null;
        Object usr = session.getAttribute("usuarioActual");
        if (usr instanceof com.hogarfix.model.Usuario) {
            var u = (com.hogarfix.model.Usuario) usr;
            var opt = clienteService.buscarPorEmail(u.getEmail());
            if (opt.isPresent()) cliente = opt.get();
        }

        if (cliente == null && principal != null) {
            var opt2 = clienteService.buscarPorEmail(principal.getName());
            if (opt2.isPresent()) cliente = opt2.get();
        }

        if (cliente == null) {
            return "redirect:/auth/login";
        }

        List<Pago> pagos = pagoService.listarPorCliente(cliente);
        // Map to DTOs to send to the view (avoid exposing JPA entities)
        var pagosDto = pagos.stream().map(com.hogarfix.mapper.PagoMapper::toDTO).toList();
        if (logger.isInfoEnabled()) {
            logger.info("/pagos requested by cliente.id={}. pagos.size={}", cliente.getIdCliente(), pagos == null ? 0 : pagos.size());
            if (pagos != null && !pagos.isEmpty()) {
                Pago first = pagos.get(0);
                logger.info("first pago id={} servicioPresent={} clientePresent={}",
                        first.getIdPago(),
                        first.getServicio() != null,
                        first.getCliente() != null);
                if (first.getServicio() != null) {
                    var s = first.getServicio();
                    logger.info("servicio id={} descripcionPresent={} categoriaPresent={} tecnicoPresent={}",
                            s.getIdServicio(),
                            s.getDescripcion() != null && !s.getDescripcion().isBlank(),
                            s.getCategoria() != null,
                            s.getTecnico() != null);
                }
            }
        }
        if (pagoId != null) {
            // move selected pago to the front if present (operate on DTOs)
            pagosDto = pagosDto.stream().sorted((a, b) -> {
                if (a.getIdPago() != null && a.getIdPago().equals(pagoId)) return -1;
                if (b.getIdPago() != null && b.getIdPago().equals(pagoId)) return 1;
                return 0;
            }).toList();
        }
        model.addAttribute("pagos", pagosDto);
        model.addAttribute("cliente", cliente);
        model.addAttribute("mpConfigurado", mercadoPagoService.estaConfigurado());
        // Exponer token CSRF para llamadas fetch desde JS
        Object csrf = request.getAttribute("_csrf");
        if (csrf != null) model.addAttribute("_csrf", csrf);
        return "pagos";
    }

    /**
     * El cliente confirma que ya pagó en efectivo directamente al técnico.
     * No pasa por ninguna pasarela: es una autodeclaración del cliente.
     */
    @PostMapping("/pagos/{id}/confirmar-efectivo")
    public RedirectView confirmarPagoEfectivo(@PathVariable("id") Long id, Principal principal,
            HttpSession session, RedirectAttributes redirectAttributes) {

        Cliente cliente = null;
        Object usr = session.getAttribute("usuarioActual");
        if (usr instanceof com.hogarfix.model.Usuario) {
            var u = (com.hogarfix.model.Usuario) usr;
            var opt = clienteService.buscarPorEmail(u.getEmail());
            if (opt.isPresent()) cliente = opt.get();
        }
        if (cliente == null && principal != null) {
            var opt2 = clienteService.buscarPorEmail(principal.getName());
            if (opt2.isPresent()) cliente = opt2.get();
        }
        if (cliente == null) {
            return new RedirectView("/auth/login");
        }

        var pagoOpt = pagoService.buscarPorIdConDetalle(id);
        if (pagoOpt.isEmpty() || pagoOpt.get().getCliente() == null
                || !pagoOpt.get().getCliente().getIdCliente().equals(cliente.getIdCliente())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo confirmar el pago");
            return new RedirectView("/pagos");
        }

        pagoService.marcarPagadoConReferencia(id, "efectivo", "CONFIRMADO_POR_CLIENTE");
        redirectAttributes.addFlashAttribute("mensajeExito", "¡Gracias! Confirmamos tu pago en efectivo.");
        return new RedirectView("/");
    }
}