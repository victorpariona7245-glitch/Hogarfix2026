package com.hogarfix.controller;

import com.hogarfix.model.Servicio;
import com.hogarfix.model.Categoria;
import com.hogarfix.model.Cliente;
import com.hogarfix.model.Usuario;
import com.hogarfix.service.ServicioService;
import com.hogarfix.service.CategoriaService;
import com.hogarfix.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/servicios")
public class ServicioController {

    private final ServicioService servicioService;
    private final CategoriaService categoriaService;
    private final com.hogarfix.service.TecnicoService tecnicoService;
    private final ClienteService clienteService;

    private final com.hogarfix.service.ServicioService servicioServiceInternal;
    private final com.hogarfix.service.PagoService pagoService;
    private final com.hogarfix.service.EmailService emailService;

    @GetMapping
    public String listarServicios(Model model) {
        model.addAttribute("servicios", servicioService.listarServicios());
        return "servicios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevoServicio(Model model) {
        model.addAttribute("servicio", new Servicio());
        return "servicios/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Servicio servicio) {
        servicioService.registrarServicio(servicio);
        return "redirect:/servicios";
    }

    /**
     * Endpoint para solicitar un servicio desde la página pública (index).
     * Solo clientes autenticados pueden crear una solicitud.
     */
    @PostMapping("/solicitar")
    public String solicitarServicio(@RequestParam("idCategoria") Long idCategoria,
                                    @RequestParam("urgency") String urgency,
                                    @RequestParam(value = "name", required = false) String name,
                                    @RequestParam("phone") String phone,
                                    @RequestParam("address") String address,
                                    @RequestParam(value = "date", required = false) String date,
                                    @RequestParam("description") String description,
                                    @RequestParam(value = "idTecnico", required = false) Long idTecnico,
                                    @RequestParam(value = "metodoPago", required = false, defaultValue = "tarjeta") String metodoPago,
                                    Principal principal,
                                    HttpSession session,
                                    Model model) {
        // comprobar autenticación
        if (principal == null) {
            return "redirect:/auth/login";
        }

        // resolver cliente a partir del principal o de la sesión
        Cliente cliente = null;
        Object usr = session.getAttribute("usuarioActual");
        if (usr instanceof Usuario) {
            Usuario u = (Usuario) usr;
            var opt = clienteService.buscarPorEmail(u.getEmail());
            if (opt.isPresent()) cliente = opt.get();
        }

        if (cliente == null) {
            // intentar buscar por principal name
            var opt2 = clienteService.buscarPorEmail(principal.getName());
            if (opt2.isPresent()) cliente = opt2.get();
        }

        if (cliente == null) {
            // no es cliente -> forzar login
            return "redirect:/auth/login";
        }

        Categoria categoria = categoriaService.buscarPorId(idCategoria)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

    Servicio s = new Servicio();
    s.setCliente(cliente);
    s.setMetodoPagoPreferido(metodoPago);
        // si el cliente seleccionó un técnico desde la lista de técnicos, asignarlo
        if (idTecnico != null) {
            tecnicoService.buscarPorId(idTecnico).ifPresent(s::setTecnico);
        } else {
            s.setTecnico(null); // asignación posterior por sistema
        }
    s.setCategoria(categoria);
    s.setDescripcion(description);
    s.setTelefono(phone);
    s.setDireccionServicio(address);
    s.setUrgencia(urgency);
        s.setMonto(BigDecimal.ZERO);
        s.setEstado("PENDIENTE");
        s.setFechaSolicitud(LocalDateTime.now());

        servicioService.registrarServicio(s);

        // Notificar al técnico (si ya quedó asignado) que le llegó una nueva solicitud
        try {
            if (s.getTecnico() != null && s.getTecnico().getUsuario() != null) {
                String emailTecnico = s.getTecnico().getUsuario().getEmail();
                String nombreTecnico = s.getTecnico().getNombres();
                String nombreCliente = cliente.getNombres() + " " + cliente.getApellidoPaterno();
                emailService.sendNuevaSolicitudEmail(emailTecnico, nombreTecnico, nombreCliente,
                        categoria.getNombre(), urgency, description);
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ServicioController.class)
                    .warn("No se pudo notificar al técnico la nueva solicitud: {}", ex.getMessage());
        }

        // Añadir datos necesarios al modelo para renderizar index con mensaje de éxito
        model.addAttribute("successMessage", "Solicitud enviada correctamente. Revisar en Mi Perfil > Solicitudes.");
        model.addAttribute("categorias", categoriaService.listarCategorias());

        return "index";
    }

    // --- Acciones del técnico sobre una solicitud ---
    @PostMapping("/{id}/aceptar")
    public String aceptarSolicitud(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        // verificar que el técnico autenticado es el asignado
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return "redirect:/tecnicos/panel?error=notfound";
        var s = servOpt.get();
        if (s.getTecnico() == null) return "redirect:/tecnicos/panel?error=noasignado";

        try {
            var tecnico = tecnicoService.obtenerPorEmail(principal.getName());
            if (tecnico == null || !tecnico.getIdTecnico().equals(s.getTecnico().getIdTecnico())) {
                return "redirect:/tecnicos/panel?error=forbidden";
            }
        } catch (Exception ex) {
            return "redirect:/tecnicos/panel?error=forbidden";
        }

        servicioServiceInternal.marcarEnProgreso(id);
        return "redirect:/tecnicos/panel";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelarSolicitud(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return "redirect:/tecnicos/panel?error=notfound";
        var s = servOpt.get();
        // permitir cancelar solo si el técnico coincide
        if (s.getTecnico() == null) return "redirect:/tecnicos/panel?error=noasignado";
        try {
            var tecnico = tecnicoService.obtenerPorEmail(principal.getName());
            if (tecnico == null || !tecnico.getIdTecnico().equals(s.getTecnico().getIdTecnico())) {
                return "redirect:/tecnicos/panel?error=forbidden";
            }
        } catch (Exception ex) {
            return "redirect:/tecnicos/panel?error=forbidden";
        }

        servicioServiceInternal.marcarCancelado(id);

        // Notificar al cliente que su solicitud fue rechazada
        try {
            if (s.getCliente() != null && s.getCliente().getUsuario() != null) {
                String emailCliente = s.getCliente().getUsuario().getEmail();
                String nombreCliente = s.getCliente().getNombres();
                String nombreTecnico = (s.getTecnico() != null)
                        ? s.getTecnico().getNombres() + " " + s.getTecnico().getApellidoPaterno()
                        : null;
                String categoria = (s.getCategoria() != null) ? s.getCategoria().getNombre() : null;
                emailService.sendSolicitudRechazadaEmail(emailCliente, nombreCliente, nombreTecnico, categoria);
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ServicioController.class)
                    .warn("No se pudo notificar al cliente el rechazo del servicio {}: {}", id, ex.getMessage());
        }

        return "redirect:/tecnicos/panel";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizarSolicitud(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return "redirect:/tecnicos/panel?error=notfound";
        var s = servOpt.get();
        if (s.getTecnico() == null) return "redirect:/tecnicos/panel?error=noasignado";
        try {
            var tecnico = tecnicoService.obtenerPorEmail(principal.getName());
            if (tecnico == null || !tecnico.getIdTecnico().equals(s.getTecnico().getIdTecnico())) {
                return "redirect:/tecnicos/panel?error=forbidden";
            }
        } catch (Exception ex) {
            return "redirect:/tecnicos/panel?error=forbidden";
        }

        var updatedOpt = servicioServiceInternal.marcarFinalizado(id);
        if (updatedOpt.isPresent()) {
            var updated = updatedOpt.get();
            try {
                // crear pago pendiente para el cliente con monto estático S/120
                java.math.BigDecimal monto = java.math.BigDecimal.valueOf(120L);
                String metodoElegido = (updated.getMetodoPagoPreferido() != null
                        && !updated.getMetodoPagoPreferido().isBlank())
                                ? updated.getMetodoPagoPreferido()
                                : "tarjeta";
                com.hogarfix.model.Pago pago = com.hogarfix.model.Pago.builder()
                        .servicio(updated)
                        .cliente(updated.getCliente())
                        .monto(monto)
                        .metodoPago(metodoElegido)
                        .fechaPago(java.time.LocalDateTime.now())
                        .estado("PENDIENTE")
                        .build();
                pagoService.registrarPago(pago);
            } catch (Exception ex) {
                // no queremos impedir la finalización si falla la creación del pago, solo loguear
                org.slf4j.LoggerFactory.getLogger(ServicioController.class).warn("No se pudo crear pago tras finalizar servicio {}: {}", id, ex.getMessage());
            }
        }

        return "redirect:/tecnicos/panel";
    }
}