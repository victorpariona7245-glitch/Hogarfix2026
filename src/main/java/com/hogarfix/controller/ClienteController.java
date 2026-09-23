package com.hogarfix.controller;

import com.hogarfix.model.Ciudad;
import com.hogarfix.model.Cliente;
import com.hogarfix.model.Direccion;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Usuario;
import com.hogarfix.service.CiudadService;
import com.hogarfix.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clientes")
public class ClienteController {

    private static final Logger logger = LoggerFactory.getLogger(ClienteController.class);

    private final ClienteService clienteService;
    private final CiudadService ciudadService;
    private final AuthenticationManager authenticationManager;
    private final com.hogarfix.service.ServicioService servicioService;
    private final com.hogarfix.service.PagoService pagoService;
    private final com.hogarfix.service.EmailService emailService;
    private final com.hogarfix.service.CalificacionService calificacionService;

    @GetMapping
    public String listarClientes(Model model) {
        List<Cliente> clientes = clienteService.listarClientes();
        model.addAttribute("clientes", clientes);
        return "cliente/lista";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Principal principal) {
        // Comprobar que el usuario está autenticado
        if (principal == null) {
            return "redirect:/auth/login";
        }
        try {
            // Obtener cliente a partir del principal (puede ser email o username)
            String name = principal.getName();
            Cliente cliente = null;
            var optByEmail = clienteService.buscarPorEmail(name);
            if (optByEmail.isPresent()) {
                cliente = optByEmail.get();
            } else {
                var optByUsername = clienteService.buscarPorUsername(name);
                if (optByUsername.isPresent())
                    cliente = optByUsername.get();
            }

            if (cliente == null) {
                // No es cliente o no lo encontramos — redirigir al inicio o mostrar mensaje
                return "redirect:/";
            }
            model.addAttribute("cliente", cliente);
            model.addAttribute("ciudades", ciudadService.listarCiudades());

            // añadir solicitudes pendientes del cliente al modelo
            try {
                var servicios = servicioService.listarPorCliente(cliente);
                var pendientes = servicios.stream()
                        .filter(s -> "PENDIENTE".equalsIgnoreCase(s.getEstado()))
                        .toList();
                model.addAttribute("serviciosPendientes", pendientes);

                // servicios en proceso (técnico ya aceptó) — aquí se habilita el chat
                var enProceso = servicios.stream()
                        .filter(s -> "EN_PROCESO".equalsIgnoreCase(s.getEstado()))
                        .toList();
                model.addAttribute("serviciosEnProceso", enProceso);

                // servicios finalizados que el cliente aún no ha calificado
                var finalizadosSinCalificar = servicios.stream()
                        .filter(s -> "FINALIZADO".equalsIgnoreCase(s.getEstado()))
                        .filter(s -> s.getCalificaciones() == null || s.getCalificaciones().isEmpty())
                        .toList();
                model.addAttribute("serviciosPorCalificar", finalizadosSinCalificar);
            } catch (Exception ex) {
                // no bloquear la vista por errores al recuperar servicios
                model.addAttribute("serviciosPendientes", java.util.List.of());
                model.addAttribute("serviciosEnProceso", java.util.List.of());
                model.addAttribute("serviciosPorCalificar", java.util.List.of());
            }
            // añadir pagos pendientes del cliente al modelo (creados cuando el técnico
            // finaliza)
            try {
                var pagos = pagoService.listarPorCliente(cliente);
                var pagosPendientes = pagos.stream()
                        .filter(p -> p.getEstado() != null && p.getEstado().equalsIgnoreCase("PENDIENTE"))
                        .toList();
                model.addAttribute("pagosPendientes", pagosPendientes);
            } catch (Exception ex) {
                model.addAttribute("pagosPendientes", java.util.List.of());
            }
            return "cliente/perfil";
        } catch (Exception ex) {
            logger.error("Error al mostrar perfil del cliente", ex);
            return "redirect:/auth/login?error=perfil_error";
        }
    }

    @PostMapping("/calificar")
    public String calificarServicio(
            @RequestParam Long idServicio,
            @RequestParam int puntuacion,
            @RequestParam(required = false) String observacion,
            Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        try {
            String name = principal.getName();
            Cliente cliente = clienteService.buscarPorEmail(name)
                    .or(() -> clienteService.buscarPorUsername(name))
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            Servicio servicio = servicioService.buscarPorId(idServicio)
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

            calificacionService.calificarServicio(servicio, cliente, puntuacion, observacion);

            redirectAttributes.addFlashAttribute("mensajeExito", "¡Gracias por tu calificación!");
        } catch (RuntimeException ex) {
            logger.warn("Error al calificar servicio: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Error inesperado al calificar servicio", ex);
            redirectAttributes.addFlashAttribute("mensajeError", "Ocurrió un error al registrar tu calificación");
        }

        return "redirect:/clientes/perfil";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        // Inicializamos objetos anidados para que Thymeleaf los pueda enlazar
        Cliente cliente = new Cliente();
        cliente.setUsuario(new Usuario());
        cliente.setDireccion(new Direccion());
        model.addAttribute("cliente", cliente);
        // Pasamos lista de ciudades para el select
        model.addAttribute("ciudades", ciudadService.listarCiudades());
        return "cliente/registro";
    }

    @PostMapping("/registro")
    public String registrarCliente(@ModelAttribute Cliente cliente, Model model, HttpSession session) {
        String email = cliente != null && cliente.getUsuario() != null ? cliente.getUsuario().getEmail()
                : "desconocido";
        logger.info("Iniciando registro de cliente: email={}", email);

        try {
            // Aseguramos username a partir del email si no se proporcionó
            if (cliente.getUsuario() != null
                    && (cliente.getUsuario().getUsername() == null || cliente.getUsuario().getUsername().isEmpty())) {
                cliente.getUsuario().setUsername(cliente.getUsuario().getEmail());
            }

            // Resolver la ciudad seleccionada (el binding deja el id en
            // direccion.ciudad.idCiudad)
            if (cliente.getDireccion() != null && cliente.getDireccion().getCiudad() != null) {
                Long idCiudad = cliente.getDireccion().getCiudad().getIdCiudad();
                if (idCiudad != null) {
                    Optional<Ciudad> ciudadOpt = ciudadService.buscarPorId(idCiudad);
                    if (ciudadOpt.isPresent()) {
                        cliente.getDireccion().setCiudad(ciudadOpt.get());
                    } else {
                        logger.warn("Ciudad seleccionada no válida para cliente: {}", email);
                        throw new RuntimeException("Ciudad seleccionada no válida");
                    }
                } else {
                    logger.warn("No se seleccionó ciudad para cliente: {}", email);
                    throw new RuntimeException("Seleccione una ciudad");
                }
            } else {
                logger.warn("Dirección incompleta para cliente: {}", email);
                throw new RuntimeException("Dirección incompleta");
            }

            // Guarda el cliente y luego autentica la sesión automáticamente
            String rawPassword = cliente.getUsuario().getPassword();
            Cliente clienteGuardado = clienteService.registrarCliente(cliente);
            logger.info("Cliente registrado exitosamente: email={}, id={}", email, clienteGuardado.getIdCliente());

            // Autenticar usando AuthenticationManager para establecer SecurityContext
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    clienteGuardado.getUsuario().getEmail(), rawPassword);
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.info("Cliente autenticado automáticamente tras registro: email={}", email);
            // Persist SecurityContext in HTTP session so Spring Security recognizes the
            // login across requests
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Guardar en sesión el usuario y un nombre de visualización para la navbar
            session.setAttribute("usuarioActual", clienteGuardado.getUsuario());
            session.setAttribute("usuarioNombre",
                    clienteGuardado.getNombres() + " " + clienteGuardado.getApellidoPaterno());

            // Enviar email de bienvenida (EmailService maneja errores internamente)
            emailService.sendWelcomeEmail(clienteGuardado.getUsuario().getEmail(), clienteGuardado.getNombres());

            return "redirect:/";
        } catch (RuntimeException e) {
            logger.error("Error al registrar cliente: email={}, causa={}", email, e.getMessage());
            model.addAttribute("error", e.getMessage());
            // volver a cargar ciudades para el formulario en caso de error
            model.addAttribute("ciudades", ciudadService.listarCiudades());
            return "cliente/registro";
        }
    }

    @PostMapping("/actualizar-perfil")
    @ResponseBody
    public Map<String, Object> actualizarPerfil(
            @RequestParam Long idCliente,
            @RequestParam String nombres,
            @RequestParam String apellidoPaterno,
            @RequestParam String apellidoMaterno,
            @RequestParam String telefono,
            @RequestParam String email,
            @RequestParam String calle,
            @RequestParam String numero,
            @RequestParam(required = false) String referencia,
            @RequestParam Long ciudadId,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validación de sesión
            if (principal == null) {
                response.put("success", false);
                response.put("message", "Sesión expirada");
                return response;
            }

            // Obtener cliente desde BD
            Optional<Cliente> opt = clienteService.buscarPorId(idCliente);
            if (opt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Cliente no encontrado");
                return response;
            }

            Cliente cliente = opt.get();

            // Validación de seguridad (solo puede editarse a sí mismo)
            if (!principal.getName().equalsIgnoreCase(cliente.getUsuario().getEmail())) {
                response.put("success", false);
                response.put("message", "No autorizado");
                return response;
            }

            // Actualizar datos
            cliente.setNombres(nombres);
            cliente.setApellidoPaterno(apellidoPaterno);
            cliente.setApellidoMaterno(apellidoMaterno);
            cliente.setTelefono(telefono);

            // Actualizar usuario
            Usuario usuario = cliente.getUsuario();
            usuario.setEmail(email);
            usuario.setUsername(email); // Mantener autenticación por email

            cliente.getUsuario().setPassword(cliente.getUsuario().getPassword());
            // usuario.setPassword(null);

            // Actualizar dirección
            Direccion direccion = cliente.getDireccion();
            direccion.setCalle(calle);
            direccion.setNumero(numero);
            direccion.setReferencia(referencia);

            // Ciudad
            Optional<Ciudad> ciudadOpt = ciudadService.buscarPorId(ciudadId);
            ciudadOpt.ifPresent(direccion::setCiudad);

            // Guardar
            clienteService.actualizarCliente(cliente);

            response.put("success", true);
            response.put("message", "Perfil actualizado correctamente");
            return response;

        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("success", false);
            response.put("message", "Error inesperado al actualizar");
            return response;
        }
    }

}