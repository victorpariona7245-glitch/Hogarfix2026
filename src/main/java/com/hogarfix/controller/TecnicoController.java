package com.hogarfix.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;
import com.hogarfix.model.TecnicoCategoria;
import com.hogarfix.model.Usuario;
import com.hogarfix.repository.TecnicoRepository;
import com.hogarfix.repository.UsuarioRepository;
import com.hogarfix.model.Direccion;
import com.hogarfix.model.Categoria;
import com.hogarfix.model.Ciudad;
import com.hogarfix.service.CiudadService;
import com.hogarfix.service.CategoriaService;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.hogarfix.service.ServicioService;
import com.hogarfix.service.TecnicoService;
import com.hogarfix.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/tecnicos")
public class TecnicoController {

    private final UsuarioRepository usuarioRepository;

    private final TecnicoRepository tecnicoRepository;

    private final TecnicoService tecnicoService;
    private final ServicioService servicioService; // ✅ Se inyecta correctamente
    private final CiudadService ciudadService;
    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final com.hogarfix.service.EmailService emailService;
    private final com.hogarfix.service.ReniecService reniecService;
    private final com.hogarfix.repository.PagoRepository pagoRepository;

    @GetMapping
    public String listarTecnicos(Model model) {
        List<Tecnico> tecnicos = tecnicoService.listarTecnicos();
        model.addAttribute("tecnicos", tecnicos);
        return "tecnico/lista";
    }

    @GetMapping("/login")
    public String mostrarLoginTecnico() {
        return "tecnico/logintecnico";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(new Usuario());
        tecnico.setDireccion(new Direccion());
        model.addAttribute("tecnico", tecnico);
        model.addAttribute("ciudades", ciudadService.listarCiudades());
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "tecnico/registrotec";
    }

    @GetMapping("/registro-exitoso")
    public String mostrarRegistroExitoso() {
        return "tecnico/registro-exitoso";
    }

    @PostMapping("/registro")
    public String registrarTecnico(@ModelAttribute("tecnico") Tecnico tecnico,
            @RequestParam("certificadoPdfFile") MultipartFile archivo,
            @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoFile,
            @RequestParam("categoriaIds") List<Long> categoriaIds,
            Model model) throws IOException {

        String email = tecnico != null && tecnico.getUsuario() != null ? tecnico.getUsuario().getEmail()
                : "desconocido";
        log.info("Iniciando registro de técnico: email={}", email);

        try {
            // procesar archivo (certificado)
            if (archivo != null && !archivo.isEmpty()) {
                String rutaRel = saveUploadedFile(archivo, "certificados");
                tecnico.setCertificadoPdf(rutaRel);
                log.info("Certificado guardado: {}", rutaRel);
            }

            // procesar foto de perfil (opcional)
            if (fotoFile != null && !fotoFile.isEmpty()) {
                String contentType = fotoFile.getContentType();
                if (contentType == null || !(contentType.equalsIgnoreCase("image/png")
                        || contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg"))) {
                    log.warn("Foto de perfil rechazada para técnico {}: tipo de contenido inválido ({})", email,
                            contentType);
                    model.addAttribute("error", "La foto de perfil debe ser PNG o JPG/JPEG");
                    model.addAttribute("ciudades", ciudadService.listarCiudades());
                    model.addAttribute("categorias", categoriaService.listarCategorias());
                    return "tecnico/registrotec";
                }

                String rutaRel = saveUploadedFile(fotoFile, "tecnicos");
                tecnico.setFotoPerfil(rutaRel);
                log.info("Foto de perfil guardada: {}", rutaRel);
            }

            // asegurar username desde email si no viene
            if (tecnico.getUsuario() != null
                    && (tecnico.getUsuario().getUsername() == null || tecnico.getUsuario().getUsername().isEmpty())) {
                tecnico.getUsuario().setUsername(tecnico.getUsuario().getEmail());
            }

            // Resolver ciudad seleccionada
            if (tecnico.getDireccion() != null && tecnico.getDireccion().getCiudad() != null) {
                Long idCiudad = tecnico.getDireccion().getCiudad().getIdCiudad();
                if (idCiudad != null) {
                    Optional<Ciudad> ciudadOpt = ciudadService.buscarPorId(idCiudad);
                    if (ciudadOpt.isPresent()) {
                        tecnico.getDireccion().setCiudad(ciudadOpt.get());
                    } else {
                        log.warn("Ciudad seleccionada no válida para técnico: {}", email);
                        throw new RuntimeException("Ciudad seleccionada no válida");
                    }
                } else {
                    log.warn("No se seleccionó ciudad para técnico: {}", email);
                    throw new RuntimeException("Seleccione una ciudad");
                }
            } else {
                log.warn("Dirección incompleta para técnico: {}", email);
                throw new RuntimeException("Dirección incompleta");
            }

            // validar que se haya subido el certificado (campo obligatorio en la entidad)
            if (tecnico.getCertificadoPdf() == null || tecnico.getCertificadoPdf().isEmpty()) {
                log.warn("Certificado PDF faltante para técnico: {}", email);
                model.addAttribute("error", "Debe subir un certificado PDF");
                model.addAttribute("ciudades", ciudadService.listarCiudades());
                model.addAttribute("categorias", categoriaService.listarCategorias());
                return "tecnico/registrotec";
            }

            // Validar al menos una categoría seleccionada
            if (categoriaIds == null || categoriaIds.isEmpty()) {
                log.warn("No se seleccionaron categorías para el técnico {}", email);
                model.addAttribute("error", "Debe seleccionar al menos una categoría");
                model.addAttribute("ciudades", ciudadService.listarCiudades());
                model.addAttribute("categorias", categoriaService.listarCategorias());
                return "tecnico/registrotec";
            }

            // Validar el DNI contra RENIEC (si el servicio está configurado)
            var validacionDni = reniecService.validarDni(tecnico.getDni());
            if (validacionDni.validacionDisponible() && !validacionDni.dniValido()) {
                log.warn("DNI rechazado por RENIEC para técnico {}: {}", email, validacionDni.mensaje());
                model.addAttribute("error", "El DNI ingresado no es válido: " + validacionDni.mensaje());
                model.addAttribute("ciudades", ciudadService.listarCiudades());
                model.addAttribute("categorias", categoriaService.listarCategorias());
                return "tecnico/registrotec";
            }
            if (!validacionDni.validacionDisponible()) {
                log.info("Validación RENIEC omitida para técnico {}: {}", email, validacionDni.mensaje());
            }

            // Obtener categorías desde la BD
            var categoriasSeleccionadas = categoriaIds.stream()
                    .map(categoriaService::buscarPorId)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            if (categoriasSeleccionadas.isEmpty()) {
                log.warn("IDs de categorías inválidos para técnico {}", email);
                model.addAttribute("error", "Seleccione categorías válidas");
                model.addAttribute("ciudades", ciudadService.listarCiudades());
                model.addAttribute("categorias", categoriaService.listarCategorias());
                return "tecnico/registrotec";
            }

            // Crear relación TecnicoCategoria
            List<TecnicoCategoria> relaciones = categoriasSeleccionadas.stream()
                    .map(cat -> TecnicoCategoria.builder()
                            .tecnico(tecnico)
                            .categoria(cat)
                            .build())
                    .collect(Collectors.toList());

            // Asignar al técnico
            tecnico.setCategorias(relaciones);

            Tecnico saved = tecnicoService.registrarTecnico(tecnico);

            String categoriasLog = saved.getCategorias().stream()
                    .map(tc -> tc.getCategoria().getNombre())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Sin categorías");

            log.info("Técnico registrado exitosamente: email={}, id={}, categoria={}", email, saved.getIdTecnico(),
                    categoriasLog);

            // EmailService maneja internamente excepciones, así que no es necesario
            // try/catch aquí
            emailService.sendWelcomeEmail(saved.getUsuario().getEmail(), saved.getNombres());
            return "redirect:/tecnicos/registro-exitoso";
        } catch (RuntimeException e) {
            log.error("Error al registrar técnico: email={}, causa={}", email, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("ciudades", ciudadService.listarCiudades());
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "tecnico/registrotec";
        } catch (IOException e) {
            log.error("Error de I/O al registrar técnico: email={}, causa={}", email, e.getMessage());
            model.addAttribute("error", "Error al procesar los archivos. Intente nuevamente.");
            model.addAttribute("ciudades", ciudadService.listarCiudades());
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "tecnico/registrotec";
        }
    }

    @PostMapping("/login")
    public String procesarLoginTecnico(@RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session) {
        try {
            log.info("Intento de login técnico: username={}", username);
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            // Persistir el SecurityContext en la sesión para que Spring Security
            // lo reconozca en las siguientes requests (consistente con AuthController)
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            log.info("Autenticación exitosa para usuario={}", username);

            // verificar rol TECNICO
            var usuarioOpt = usuarioService
                    .buscarPorId(((com.hogarfix.model.Usuario) auth.getPrincipal()).getIdUsuario());
            if (usuarioOpt.isPresent()) {
                Usuario u = usuarioOpt.get();
                if (u.getRol() != null && u.getRol().getTipoRol() == com.hogarfix.model.enums.TipoRol.TECNICO) {
                    session.setAttribute("usuarioActual", u);
                    // Guardar nombre para mostrar en la navbar
                    try {
                        Tecnico t = tecnicoService.obtenerPorEmail(u.getEmail());
                        if (t != null) {
                            session.setAttribute("usuarioNombre", t.getNombres() + " " + t.getApellidoPaterno());
                        } else {
                            session.setAttribute("usuarioNombre", u.getEmail());
                        }
                    } catch (Exception ex) {
                        session.setAttribute("usuarioNombre", u.getEmail());
                    }
                    // Redirigir al panel del técnico (mostrar nombre en navbar)
                    return "redirect:/tecnicos/panel";
                } else {
                    // si no es técnico, cerrar sesión y devolver error
                    SecurityContextHolder.clearContext();
                    return "redirect:/tecnicos/login?error";
                }
            }

            return "redirect:/tecnicos/login?error";
        } catch (org.springframework.security.authentication.DisabledException ex) {
            log.warn("Intento de login de técnico con cuenta pendiente de aprobación: username={}", username);
            return "redirect:/tecnicos/login?error=pendiente";
        } catch (AuthenticationException ex) {
            log.warn("Fallo de autenticación para usuario={}: {}", username, ex.getMessage());
            return "redirect:/tecnicos/login?error";
        } catch (Exception ex) {
            log.error("Error inesperado en procesarLoginTecnico para usuario={}", username, ex);
            return "redirect:/tecnicos/login?error=server";
        }
    }

    // NOTA: el endpoint GET /certificados/{nombreArchivo} ya está definido en
    // CertificadoController.java — no se duplica aquí para evitar el error
    // "Ambiguous handler methods mapped".

    @GetMapping("/fotos/{nombreArchivo}")
    public ResponseEntity<Resource> verFoto(@PathVariable String nombreArchivo) throws IOException {
        Path archivoPath = Paths.get(System.getProperty("user.dir"), "uploads", "tecnicos").resolve(nombreArchivo)
                .normalize();
        log.info("Solicitando foto: {} -> path={}", nombreArchivo, archivoPath.toAbsolutePath());
        if (!Files.exists(archivoPath) || !Files.isReadable(archivoPath)) {
            log.warn("Foto no encontrada o no legible: {}", archivoPath);
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new UrlResource(archivoPath.toUri());
        String contentType = Files.probeContentType(archivoPath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : "application/octet-stream")
                .body(recurso);
    }

    @GetMapping("/panel")
    public String panelTecnico(Model model, Principal principal) {
        Tecnico tecnico = resolveTecnicoFromPrincipal(principal);

        if (tecnico == null) {
            // no autenticado o no se encontró el tecnico -> forzar login
            return "redirect:/auth/login";
        }

        List<Servicio> servicios = servicioService.obtenerPorTecnico(tecnico.getIdTecnico());
        // Ordenar por fecha de solicitud: más recientes primero
        servicios.sort((a, b) -> {
            if (a.getFechaSolicitud() == null || b.getFechaSolicitud() == null)
                return 0;
            return b.getFechaSolicitud().compareTo(a.getFechaSolicitud());
        });

        model.addAttribute("tecnico", tecnico);
        model.addAttribute("servicios", servicios);
        model.addAttribute("categorias", categoriaService.listarCategorias());

        // --- Resumen de ganancias del mes actual ---
        try {
            java.time.LocalDateTime inicioMes = java.time.LocalDate.now()
                    .withDayOfMonth(1).atStartOfDay();
            java.math.BigDecimal totalMes = pagoRepository
                    .sumMontoPagadoPorTecnicoDesde(tecnico.getIdTecnico(), inicioMes);
            model.addAttribute("gananciasMes", totalMes);
        } catch (Exception ex) {
            log.warn("No se pudo calcular ganancias del mes: {}", ex.getMessage());
            model.addAttribute("gananciasMes", java.math.BigDecimal.ZERO);
        }

        long serviciosCompletados = servicios.stream()
                .filter(s -> "FINALIZADO".equalsIgnoreCase(s.getEstado()))
                .count();
        model.addAttribute("serviciosCompletados", serviciosCompletados);

        // --- Mapa idServicio -> estado del pago (para mostrar "Pagado" / "Pendiente") ---
        java.util.Map<Long, String> pagoEstados = new java.util.HashMap<>();
        for (Servicio s : servicios) {
            if ("FINALIZADO".equalsIgnoreCase(s.getEstado())) {
                pagoRepository.findByServicio(s).ifPresent(p -> pagoEstados.put(s.getIdServicio(), p.getEstado()));
            }
        }
        model.addAttribute("pagoEstados", pagoEstados);

        // calcular nombre de archivo de la foto y pasarlo al modelo para evitar usar
        // funciones de Thymeleaf
        String fotoNombre = null;
        try {
            if (tecnico.getFotoPerfil() != null && !tecnico.getFotoPerfil().isBlank()) {
                String ruta = tecnico.getFotoPerfil();
                int idx1 = ruta.lastIndexOf('/');
                int idx2 = ruta.lastIndexOf('\\');
                int idx = Math.max(idx1, idx2);
                fotoNombre = idx >= 0 ? ruta.substring(idx + 1) : ruta;
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer nombre de foto de perfil: {}", e.getMessage());
        }
        model.addAttribute("tecnicoFotoNombre", fotoNombre);
        return "tecnico/panel";
    }

    @PostMapping("/disponibilidad")
    public String cambiarDisponibilidad(
            @RequestParam boolean disponible,
            Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Tecnico tecnico = resolveTecnicoFromPrincipal(principal);
        if (tecnico == null) {
            return "redirect:/auth/login";
        }

        try {
            tecnicoService.cambiarDisponibilidad(tecnico.getIdTecnico(), disponible);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    disponible ? "Ahora apareces como disponible." : "Ahora apareces como no disponible.");
        } catch (RuntimeException ex) {
            log.warn("Error al cambiar disponibilidad: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo actualizar tu disponibilidad.");
        }

        return "redirect:/tecnicos/panel";
    }

    // Helper: guarda un MultipartFile en uploads/<subdir> y devuelve la ruta
    // relativa usada en la entidad
    private String saveUploadedFile(MultipartFile file, String subdir) throws IOException {
        String nombre = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", subdir);
        Files.createDirectories(dir);
        Path destino = dir.resolve(nombre);
        file.transferTo(destino.toFile());
        return "uploads/" + subdir + "/" + nombre;
    }

    // Helper: intenta resolver el Tecnico asociado al principal de forma ordenada
    private Tecnico resolveTecnicoFromPrincipal(Principal principal) {
        if (principal == null)
            return null;
        String name = principal.getName();
        try {
            Tecnico t = tecnicoService.obtenerPorEmail(name);
            if (t != null)
                return t;
        } catch (Exception ignored) {
        }

        try {
            var usuarioOpt = usuarioService.buscarPorUsername(name);
            if (usuarioOpt.isPresent()) {
                return tecnicoService.obtenerPorEmail(usuarioOpt.get().getEmail());
            }
        } catch (Exception ignored) {
        }

        try {
            var usuarioPorEmail = usuarioService.buscarPorEmail(name);
            if (usuarioPorEmail.isPresent()) {
                return tecnicoService.obtenerPorEmail(usuarioPorEmail.get().getEmail());
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void deleteFileIfExists(String path) {
        if (path == null)
            return;

        File file = new File("uploads/" + path);
        if (file.exists()) {
            file.delete();
        }
    }

    @PostMapping("/perfil/actualizar")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> actualizarPerfilTecnico(
            @RequestParam String nombres,
            @RequestParam String apellidoPaterno,
            @RequestParam String apellidoMaterno,
            @RequestParam String telefono,
            @RequestParam String correo,
            @RequestParam("categoriaIds") List<Long> categoriaIds,
            @RequestParam(value = "certificadoPdfFile", required = false) MultipartFile archivo,
            @RequestParam(value = "fotoPerfilFile", required = false) MultipartFile fotoFile,
            Principal principal) throws IOException {

        String emailAuth = principal.getName();
        log.info("Actualizando perfil técnico: {}", emailAuth);

        Tecnico tecnico = tecnicoService.obtenerPorEmailODni(emailAuth);

        // ===========================
        // ✅ DATOS PERSONALES
        // ===========================
        tecnico.setNombres(nombres);
        tecnico.setApellidoPaterno(apellidoPaterno);
        tecnico.setApellidoMaterno(apellidoMaterno);
        tecnico.setTelefono(telefono);

        tecnico.getUsuario().setEmail(correo);
        tecnico.getUsuario().setUsername(correo);

        // ===========================
        // ✅ CERTIFICADO PDF (OPCIONAL)
        // ===========================
        if (archivo != null && !archivo.isEmpty()) {
            deleteFileIfExists(tecnico.getCertificadoPdf());
            String rutaRel = saveUploadedFile(archivo, "certificados");
            tecnico.setCertificadoPdf(rutaRel);
            log.info("Nuevo certificado actualizado: {}", rutaRel);
        }

        // ===========================
        // ✅ FOTO PERFIL (OPCIONAL)
        // ===========================
        if (fotoFile != null && !fotoFile.isEmpty()) {

            String contentType = fotoFile.getContentType();
            if (contentType == null || !(contentType.equalsIgnoreCase("image/png")
                    || contentType.equalsIgnoreCase("image/jpeg")
                    || contentType.equalsIgnoreCase("image/jpg"))) {

                log.warn("Formato de foto inválido: {}", contentType);
                return ResponseEntity.badRequest().body("Formato de imagen inválido");
            }

            deleteFileIfExists(tecnico.getFotoPerfil());

            String rutaRel = saveUploadedFile(fotoFile, "tecnicos");
            tecnico.setFotoPerfil(rutaRel);
            log.info("Nueva foto actualizada: {}", rutaRel);
        }

        // ===========================
        // ✅ ACTUALIZAR CATEGORÍAS
        // ===========================
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            return ResponseEntity.badRequest().body("Debe seleccionar al menos una categoría");
        }

        // var categoriasSeleccionadas = categoriaIds.stream()
        // .map(categoriaService::buscarPorId)
        // .filter(Optional::isPresent)
        // .map(Optional::get)
        // .collect(Collectors.toList());

        // if (categoriasSeleccionadas.isEmpty()) {
        // return ResponseEntity.badRequest().body("Categorías inválidas");
        // }

        // tecnico.getCategorias().clear();

        tecnicoService.actualizarCategorias(tecnico, categoriaIds);

        // List<TecnicoCategoria> nuevasRelaciones = new ArrayList<>();

        // for (Categoria cat : categoriasSeleccionadas) {
        // TecnicoCategoria tc = TecnicoCategoria.builder()
        // .tecnico(tecnico)
        // .categoria(cat)
        // .build();
        // tecnico.getCategorias().add(tc);
        // }

        // tecnico.getCategorias().addAll(nuevasRelaciones);

        // ===========================
        // ✅ GUARDAR EN BD
        // ===========================
        tecnicoRepository.save(tecnico);
        usuarioRepository.save(tecnico.getUsuario());

        log.info("Perfil técnico actualizado correctamente: {}", correo);

        return ResponseEntity.ok().build();
    }

}