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
    private final com.hogarfix.service.MaterialService materialService;

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
                                    @RequestParam(value = "metodoPago", required = false, defaultValue = "yape") String metodoPago,
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

        // No se envían solicitudes a un técnico bloqueado por comisión vencida
        if (idTecnico != null && pagoService.tecnicoBloqueadoPorComision(idTecnico)) {
            model.addAttribute("mensajeError", "Este técnico no está recibiendo solicitudes por ahora. Elige otro técnico.");
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "index";
        }

    Servicio s = new Servicio();
    s.setCliente(cliente);
    s.setMetodoPagoPreferido("efectivo".equalsIgnoreCase(metodoPago) ? "efectivo" : "yape");
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

        servicioService.registrarSolicitudNueva(s);

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

        // Con comisión vencida sin entregar, el técnico no puede aceptar solicitudes nuevas
        if (pagoService.tecnicoBloqueadoPorComision(s.getTecnico().getIdTecnico())) {
            return "redirect:/tecnicos/panel?error=comision";
        }

        try {
            servicioServiceInternal.marcarAceptado(id);
        } catch (IllegalStateException ex) {
            return "redirect:/tecnicos/panel?error=transicion";
        }
        return "redirect:/tecnicos/panel";
    }

    /** Verifica que quien llama es el técnico asignado al servicio. */
    private boolean esTecnicoAsignado(Servicio s, Principal principal) {
        if (principal == null || s.getTecnico() == null) return false;
        try {
            var tecnico = tecnicoService.obtenerPorEmail(principal.getName());
            return tecnico != null && tecnico.getIdTecnico().equals(s.getTecnico().getIdTecnico());
        } catch (Exception ex) {
            return false;
        }
    }

    @PostMapping("/{id}/en-camino")
    public String enCamino(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return "redirect:/tecnicos/panel?error=notfound";
        if (!esTecnicoAsignado(servOpt.get(), principal)) return "redirect:/tecnicos/panel?error=forbidden";
        try {
            servicioServiceInternal.marcarEnCamino(id);
        } catch (IllegalStateException ex) {
            return "redirect:/tecnicos/panel?error=transicion";
        }
        return "redirect:/tecnicos/panel";
    }

    @PostMapping("/{id}/iniciar")
    public String iniciar(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return "redirect:/tecnicos/panel?error=notfound";
        if (!esTecnicoAsignado(servOpt.get(), principal)) return "redirect:/tecnicos/panel?error=forbidden";
        try {
            servicioServiceInternal.marcarEnProgreso(id);
        } catch (IllegalStateException ex) {
            return "redirect:/tecnicos/panel?error=transicion";
        }
        return "redirect:/tecnicos/panel";
    }

    /** El cliente cancela su propia solicitud (mientras el servicio no haya empezado). */
    @PostMapping("/{id}/cancelar-cliente")
    public String cancelarPorCliente(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return "redirect:/auth/login";
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return "redirect:/clientes/perfil?error=notfound";
        var s = servOpt.get();
        boolean esDueno = s.getCliente() != null && s.getCliente().getUsuario() != null
                && principal.getName().equalsIgnoreCase(s.getCliente().getUsuario().getEmail());
        if (!esDueno) return "redirect:/clientes/perfil?error=forbidden";
        try {
            servicioServiceInternal.cambiarEstado(id, "CANCELADO",
                    com.hogarfix.service.ServicioService.ACTOR_CLIENTE, "Cancelado por el cliente");
        } catch (IllegalStateException ex) {
            return "redirect:/clientes/perfil?error=transicion";
        }
        return "redirect:/clientes/perfil";
    }

    /** Historial de estados (JSON) visible solo para el cliente y el técnico del servicio. */
    @GetMapping("/{id}/historial")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> historial(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) return org.springframework.http.ResponseEntity.status(401).build();
        var servOpt = servicioService.buscarPorId(id);
        if (servOpt.isEmpty()) return org.springframework.http.ResponseEntity.notFound().build();
        var s = servOpt.get();
        String login = principal.getName();
        boolean esCliente = s.getCliente() != null && s.getCliente().getUsuario() != null
                && login.equalsIgnoreCase(s.getCliente().getUsuario().getEmail());
        boolean esTecnico = s.getTecnico() != null && s.getTecnico().getUsuario() != null
                && login.equalsIgnoreCase(s.getTecnico().getUsuario().getEmail());
        if (!esCliente && !esTecnico) return org.springframework.http.ResponseEntity.status(403).build();
        return org.springframework.http.ResponseEntity.ok(servicioServiceInternal.historial(id));
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

        try {
            servicioServiceInternal.marcarCancelado(id);
        } catch (IllegalStateException ex) {
            return "redirect:/tecnicos/panel?error=transicion";
        }

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
    public String finalizarSolicitud(@PathVariable("id") Long id,
                                     @RequestParam(value = "monto", required = false) BigDecimal montoCobrado,
                                     @RequestParam(value = "matDesc", required = false) java.util.List<String> matDesc,
                                     @RequestParam(value = "matPrecio", required = false) java.util.List<BigDecimal> matPrecio,
                                     @RequestParam(value = "matFoto", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> matFoto,
                                     Principal principal) {
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

        // El técnico fija cuánto cuesta el servicio (entre S/ 10 y S/ 5000)
        if (montoCobrado == null || montoCobrado.compareTo(BigDecimal.TEN) < 0
                || montoCobrado.compareTo(BigDecimal.valueOf(5000)) > 0) {
            return "redirect:/tecnicos/panel?error=monto";
        }
        montoCobrado = montoCobrado.setScale(2, java.math.RoundingMode.HALF_UP);

        // Materiales comprados por el técnico (cada uno con su comprobante): se suman al total
        java.util.List<com.hogarfix.service.MaterialService.Preparado> materiales;
        try {
            materiales = materialService.preparar(matDesc, matPrecio, matFoto);
        } catch (IllegalStateException ex) {
            return "redirect:/tecnicos/panel?error=materiales&msg="
                    + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        BigDecimal totalCobrado = montoCobrado.add(com.hogarfix.service.MaterialService.totalPreparados(materiales));
        if (totalCobrado.compareTo(BigDecimal.valueOf(8000)) > 0) {
            return "redirect:/tecnicos/panel?error=materiales&msg="
                    + java.net.URLEncoder.encode("El total no puede pasar de S/ 8000", java.nio.charset.StandardCharsets.UTF_8);
        }
        if (!materiales.isEmpty()) {
            if (!"EN_PROCESO".equalsIgnoreCase(s.getEstado())) return "redirect:/tecnicos/panel?error=transicion";
            materialService.guardar(s, materiales);
        }

        java.util.Optional<Servicio> updatedOpt;
        try {
            updatedOpt = servicioServiceInternal.marcarFinalizado(id);
        } catch (IllegalStateException ex) {
            materialService.borrarDeServicio(id);
            return "redirect:/tecnicos/panel?error=transicion";
        }
        if (updatedOpt.isPresent()) {
            var updated = updatedOpt.get();
            try {
                // crear pago pendiente para el cliente con el monto que fijó el técnico
                java.math.BigDecimal monto = totalCobrado; // mano de obra + materiales
                updated.setMonto(monto);
                servicioServiceInternal.registrarServicio(updated);
                String metodoElegido = (updated.getMetodoPagoPreferido() != null
                        && !updated.getMetodoPagoPreferido().isBlank())
                                ? updated.getMetodoPagoPreferido()
                                : "yape";
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