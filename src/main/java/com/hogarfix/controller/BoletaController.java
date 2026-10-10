package com.hogarfix.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hogarfix.config.SecurityUtils;
import com.hogarfix.model.Pago;
import com.hogarfix.repository.PagoRepository;
import com.hogarfix.service.ClienteService;
import com.hogarfix.service.PagoService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Boleta (constancia del servicio): la ven el cliente, el técnico del servicio y los administradores.
 * Es un comprobante interno de HogarFix, NO un comprobante de pago electrónico de SUNAT.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/boletas")
public class BoletaController {

    private final PagoRepository pagoRepository;
    private final PagoService pagoService;
    private final ClienteService clienteService;
    private final com.hogarfix.service.MaterialService materialService;
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> ultimosEnvios = new java.util.concurrent.ConcurrentHashMap<>();

    // Datos opcionales de la empresa (variables de entorno en Railway)
    @Value("${EMPRESA_RUC:}")
    private String empresaRuc;
    @Value("${EMPRESA_DIRECCION:}")
    private String empresaDireccion;
    @Value("${EMPRESA_TELEFONO:}")
    private String empresaTelefono;

    @Getter
    @AllArgsConstructor
    public static class Boleta {
        private String numero;
        private LocalDateTime fechaServicio;
        private LocalDateTime fechaPago;
        private String cliente;
        private String tecnico;
        private String categoria;
        private String descripcion;
        private String direccionServicio;
        private String metodo;
        private BigDecimal monto;
        private boolean pagado;
    }

    @Getter
    @AllArgsConstructor
    public static class FilaBoleta {
        private Long idServicio;
        private String numero;
        private LocalDateTime fecha;
        private String categoria;
        private String tecnico;
        private BigDecimal monto;
    }

    private static String numero(Pago p) {
        return String.format("B001-%06d", p.getIdPago());
    }

    private static String metodoTexto(String metodo) {
        if (metodo == null) return "-";
        return switch (metodo.toLowerCase()) {
            case "efectivo" -> "Efectivo";
            case "plin" -> "Plin";
            case "yape" -> "Yape";
            default -> metodo;
        };
    }

    @GetMapping("/{idServicio}")
    @Transactional(readOnly = true)
    public String ver(@PathVariable Long idServicio, Principal principal, Model model) {
        if (principal == null) return "redirect:/auth/login";

        var pagoOpt = pagoRepository.findByServicio_IdServicio(idServicio);
        if (pagoOpt.isEmpty()) return "redirect:/";
        Pago p = pagoOpt.get();
        var s = p.getServicio();

        String login = principal.getName();
        boolean esCliente = p.getCliente() != null && p.getCliente().getUsuario() != null
                && login.equalsIgnoreCase(p.getCliente().getUsuario().getEmail());
        boolean esTecnico = s != null && s.getTecnico() != null && s.getTecnico().getUsuario() != null
                && login.equalsIgnoreCase(s.getTecnico().getUsuario().getEmail());
        if (!esCliente && !esTecnico && !SecurityUtils.esAdmin()) {
            return "redirect:/";
        }
        if (s == null || !"FINALIZADO".equalsIgnoreCase(s.getEstado())) {
            return "redirect:/";
        }

        String cliente = p.getCliente() != null
                ? (p.getCliente().getNombres() + " " + p.getCliente().getApellidoPaterno()).trim() : "-";
        String tecnico = s.getTecnico() != null
                ? (s.getTecnico().getNombres() + " " + s.getTecnico().getApellidoPaterno()).trim() : "-";
        String categoria = s.getCategoria() != null ? s.getCategoria().getNombre() : "Servicio";
        boolean pagado = "PAGADO".equalsIgnoreCase(p.getEstado());

        model.addAttribute("b", new Boleta(numero(p), s.getFechaFinalizacion(), pagado ? p.getFechaPago() : null,
                cliente, tecnico, categoria, s.getDescripcion(), s.getDireccionServicio(),
                metodoTexto(p.getMetodoPago()), p.getMonto(), pagado));
        model.addAttribute("idServicio", idServicio);
        var materiales = materialService.listar(idServicio);
        model.addAttribute("materiales", materiales);
        model.addAttribute("manoObra", p.getMonto().subtract(com.hogarfix.service.MaterialService.total(materiales)));
        model.addAttribute("ruc", empresaRuc);
        model.addAttribute("direccion", empresaDireccion);
        model.addAttribute("telefono", empresaTelefono);
        model.addAttribute("porcentajeInfo", pagoService.getPorcentajeComision());
        return "boletas/boleta";
    }

    /** El cliente pide que le reenvíen la boleta a su correo (máximo una vez por minuto por servicio). */
    @PostMapping("/{idServicio}/enviar")
    @Transactional(readOnly = true)
    public String enviarPorCorreo(@PathVariable Long idServicio, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/auth/login";
        var pagoOpt = pagoRepository.findByServicio_IdServicio(idServicio);
        if (pagoOpt.isEmpty()) return "redirect:/";
        Pago p = pagoOpt.get();
        boolean esCliente = p.getCliente() != null && p.getCliente().getUsuario() != null
                && principal.getName().equalsIgnoreCase(p.getCliente().getUsuario().getEmail());
        if (!esCliente || !"PAGADO".equalsIgnoreCase(p.getEstado())) {
            ra.addFlashAttribute("mensajeError", "Solo puedes enviarte la boleta de un servicio ya pagado.");
            return "redirect:/boletas/" + idServicio;
        }
        long ahora = System.currentTimeMillis();
        Long antes = ultimosEnvios.get(idServicio);
        if (antes != null && ahora - antes < 60_000) {
            ra.addFlashAttribute("mensajeError", "Ya te la enviamos hace un momento. Revisa tu correo (y la carpeta de spam).");
            return "redirect:/boletas/" + idServicio;
        }
        ultimosEnvios.put(idServicio, ahora);
        pagoService.enviarBoleta(p);
        ra.addFlashAttribute("mensajeExito", "Te enviamos la boleta a " + p.getCliente().getUsuario().getEmail() + ".");
        return "redirect:/boletas/" + idServicio;
    }

    /** Lista de boletas del cliente (servicios ya pagados). */
    @GetMapping
    @Transactional(readOnly = true)
    public String listar(Principal principal, Model model) {
        if (principal == null) return "redirect:/auth/login";
        var clienteOpt = clienteService.buscarPorEmail(principal.getName());
        if (clienteOpt.isEmpty()) return "redirect:/";

        List<FilaBoleta> filas = new ArrayList<>();
        for (Pago p : pagoService.listarPorCliente(clienteOpt.get())) {
            if (!"PAGADO".equalsIgnoreCase(p.getEstado()) || p.getServicio() == null) continue;
            var s = p.getServicio();
            filas.add(new FilaBoleta(s.getIdServicio(), numero(p), p.getFechaPago(),
                    s.getCategoria() != null ? s.getCategoria().getNombre() : "Servicio",
                    s.getTecnico() != null ? (s.getTecnico().getNombres() + " " + s.getTecnico().getApellidoPaterno()).trim() : "-",
                    p.getMonto()));
        }
        filas.sort((a, b) -> {
            if (a.getFecha() == null) return 1;
            if (b.getFecha() == null) return -1;
            return b.getFecha().compareTo(a.getFecha());
        });
        model.addAttribute("filas", filas);
        return "boletas/lista";
    }
}
