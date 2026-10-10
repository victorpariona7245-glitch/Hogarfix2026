package com.hogarfix.service;

import com.hogarfix.model.Pago;
import com.hogarfix.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import com.hogarfix.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private static final Pattern CODIGO_OPERACION = Pattern.compile("^[A-Za-z0-9\\-]{4,30}$");

    private final PagoRepository pagoRepository;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    /** Porcentaje que se queda HogarFix por servicio (variable de entorno COMISION_PORCENTAJE, por defecto 10). */
    @Value("${COMISION_PORCENTAJE:10}")
    private BigDecimal porcentajeComision;

    /**
     * Días que tiene un técnico para entregar la comisión de un pago ya cobrado. Pasado ese plazo no puede
     * aceptar solicitudes nuevas hasta ponerse al día. Variable COMISION_DIAS_GRACIA (0 = sin bloqueo).
     */
    @Value("${COMISION_DIAS_GRACIA:7}")
    private int diasGracia;

    public int getDiasGracia() {
        return Math.max(0, diasGracia);
    }

    /** ¿Tiene el técnico comisiones vencidas sin entregar? (si es así, no puede recibir solicitudes nuevas) */
    @Transactional(readOnly = true)
    public boolean tecnicoBloqueadoPorComision(Long idTecnico) {
        if (idTecnico == null || getDiasGracia() == 0) return false;
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().minusDays(getDiasGracia());
        return pagoRepository.findComisionesPendientesDeTecnico(idTecnico).stream()
                .anyMatch(p -> p.getFechaPago() != null && p.getFechaPago().isBefore(limite));
    }

    /** Recordatorio diario (9:00, hora de Lima) a técnicos con comisión pendiente o por vencer. */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 9 * * *", zone = "America/Lima")
    @Transactional
    public void recordarComisionesPendientes() {
        int gracia = getDiasGracia();
        if (gracia == 0) return;
        java.util.Map<Long, java.util.List<Pago>> porTecnico = new java.util.HashMap<>();
        for (Pago p : pagoRepository.findTodasComisionesPendientes()) {
            porTecnico.computeIfAbsent(p.getServicio().getTecnico().getIdTecnico(), k -> new java.util.ArrayList<>())
                    .add(p);
        }
        java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        for (var entrada : porTecnico.entrySet()) {
            try {
                var pagos = entrada.getValue();
                var tecnico = pagos.get(0).getServicio().getTecnico();
                BigDecimal total = pagos.stream().map(Pago::getComision).reduce(BigDecimal.ZERO, BigDecimal::add);
                long dias = pagos.stream().filter(p -> p.getFechaPago() != null)
                        .mapToLong(p -> java.time.temporal.ChronoUnit.DAYS.between(p.getFechaPago().toLocalDate(), hoy))
                        .max().orElse(0);
                if (dias < Math.max(1, gracia - 3) || tecnico.getUsuario() == null) continue;
                String msg = dias >= gracia
                        ? "Tienes S/ " + total + " de comisión vencida. Mientras no la entregues a HogarFix no puedes aceptar solicitudes nuevas"
                        : "Tienes S/ " + total + " de comisión por entregar a HogarFix. Te quedan " + (gracia - dias)
                                + " día(s) antes de que se bloqueen las solicitudes nuevas";
                notificacionService.crearSiNoHayPendiente(tecnico.getUsuario(), "COMISION", msg,
                        "/tecnicos/panel", tecnico.getIdTecnico());
            } catch (Exception ex) {
                log.warn("No se pudo recordar la comisión pendiente: {}", ex.getMessage());
            }
        }
    }

    public BigDecimal getPorcentajeComision() {
        BigDecimal p = porcentajeComision == null ? BigDecimal.TEN : porcentajeComision;
        if (p.signum() < 0) p = BigDecimal.ZERO;
        if (p.compareTo(BigDecimal.valueOf(50)) > 0) p = BigDecimal.valueOf(50);
        return p;
    }

    /** Comisión de la plataforma sobre un monto (redondeada a 2 decimales). */
    public BigDecimal calcularComision(BigDecimal monto) {
        if (monto == null) return BigDecimal.ZERO;
        return monto.multiply(getPorcentajeComision())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Fija comisión y parte del técnico según el monto actual (si aún no estaban fijadas). */
    private void aplicarComision(Pago p) {
        if (p.getMonto() != null && p.getComision() == null) {
            BigDecimal c = calcularComision(p.getMonto());
            p.setComision(c);
            p.setMontoTecnico(p.getMonto().subtract(c));
        }
    }

    public Pago registrarPago(Pago pago) {
        aplicarComision(pago);
        return pagoRepository.save(pago);
    }

    public List<Pago> listarPagos() {
        return pagoRepository.findAll();
    }

    /**
     * Lista todos los pagos inicializando las relaciones necesarias para
     * reportes/exportación (cliente, servicio y técnico asignado), evitando
     * errores de carga diferida (LazyInitializationException) fuera de la
     * vista Thymeleaf.
     */
    @Transactional(readOnly = true)
    public List<Pago> listarPagosParaExportar() {
        List<Pago> pagos = pagoRepository.findAll();
        for (Pago p : pagos) {
            if (p.getCliente() != null) {
                p.getCliente().getNombres();
                p.getCliente().getApellidoPaterno();
            }
            if (p.getServicio() != null) {
                p.getServicio().getDescripcion();
                if (p.getServicio().getTecnico() != null) {
                    p.getServicio().getTecnico().getNombres();
                    p.getServicio().getTecnico().getApellidoPaterno();
                }
            }
        }
        return pagos;
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPorCliente(com.hogarfix.model.Cliente cliente) {
        List<Pago> pagos = pagoRepository.findByCliente(cliente);
        // Inicializar todas las relaciones lazy dentro de la transacción
        for (Pago p : pagos) {
            if (p.getServicio() != null) {
                // Tocar los campos lazy de Servicio para inicializarlos
                p.getServicio().getIdServicio();
                p.getServicio().getDescripcion();
                p.getServicio().getMonto();
                
                // Inicializar Tecnico si existe
                if (p.getServicio().getTecnico() != null) {
                    p.getServicio().getTecnico().getNombres();
                    p.getServicio().getTecnico().getApellidoPaterno();
                    p.getServicio().getTecnico().getApellidoMaterno();
                }
                
                // Inicializar Categoria si existe
                if (p.getServicio().getCategoria() != null) {
                    p.getServicio().getCategoria().getNombre();
                }
            }
        }
        return pagos;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Pago> buscarPorIdConDetalle(Long id) {
        var opt = pagoRepository.findById(id);
        opt.ifPresent(p -> {
            if (p.getCliente() != null && p.getCliente().getUsuario() != null) {
                p.getCliente().getUsuario().getEmail();
            }
            if (p.getServicio() != null) {
                if (p.getServicio().getCategoria() != null) {
                    p.getServicio().getCategoria().getNombre();
                }
            }
        });
        return opt;
    }

    @Transactional
    public java.util.Optional<Pago> marcarPagado(Long id) {
        return pagoRepository.findById(id).map(p -> {
            aplicarComision(p);
            p.setEstado("PAGADO");
            p.setFechaPago(java.time.LocalDateTime.now());
            // opcional: actualizar servicio si hace falta
            try {
                if (p.getServicio() != null) {
                    var s = p.getServicio();
                    s.setMonto(p.getMonto()); // asegurar monto
                    // no cambiar estado del servicio aquí
                }
            } catch (Exception ignored) {}
            return pagoRepository.save(p);
        });
    }

    /**
     * Marca un pago como PAGADO tras un cargo real y exitoso en Culqi,
     * guardando la referencia del cargo para trazabilidad/auditoría.
     */
    @Transactional
    public java.util.Optional<Pago> marcarPagadoConReferencia(Long id, String metodoPago, String referenciaCulqi) {
        return pagoRepository.findById(id).map(p -> {
            if ("PAGADO".equalsIgnoreCase(p.getEstado())) {
                return p; // ya estaba pagado (p. ej. llegó el webhook y luego el retorno): no repetir
            }
            aplicarComision(p);
            p.setEstado("PAGADO");
            p.setFechaPago(java.time.LocalDateTime.now());
            p.setMetodoPago(metodoPago);
            p.setReferenciaPago(referenciaCulqi);
            try {
                if (p.getServicio() != null) {
                    p.getServicio().setMonto(p.getMonto());
                }
            } catch (Exception ignored) {}
            Pago guardado = pagoRepository.save(p);
            notificarPagado(guardado);
            return guardado;
        });
    }

    // ---------------------------------------------------------------------------------
    // Pagos manuales (Yape / Plin): el cliente informa su nº de operación y un admin lo verifica
    // ---------------------------------------------------------------------------------

    /** El cliente informa que pagó por Yape/Plin. Pasa a EN_REVISION hasta que un admin lo confirme. */
    @Transactional
    public Pago reportarPagoManual(Long id, String metodo, String codigo) {
        Pago p = pagoRepository.findById(id).orElseThrow(() -> new IllegalStateException("Pago no encontrado"));
        if (!"PENDIENTE".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Este pago ya no está pendiente");
        }
        String m = metodo == null ? "" : metodo.trim().toLowerCase();
        if (!m.equals("yape") && !m.equals("plin")) {
            throw new IllegalStateException("Método de pago no válido");
        }
        String c = codigo == null ? "" : codigo.trim();
        if (!CODIGO_OPERACION.matcher(c).matches()) {
            throw new IllegalStateException("Ingresa el número de operación (4 a 30 letras, números o guiones)");
        }
        if (pagoRepository.existsByCodigoOperacionAndIdPagoNot(c, id)) {
            throw new IllegalStateException("Ese número de operación ya fue usado en otro pago");
        }
        aplicarComision(p);
        p.setMetodoPago(m);
        p.setCodigoOperacion(c);
        p.setReferenciaPago(c);
        p.setNotaRevision(null);
        p.setEstado("EN_REVISION");
        Pago guardado = pagoRepository.save(p);

        try {
            String cliente = p.getCliente() != null ? p.getCliente().getNombres() : "Un cliente";
            String texto = cliente + " informó un pago por " + m.toUpperCase() + " de S/ " + p.getMonto()
                    + " (operación " + c + "). Revísalo en tu Yape/Plin y apruébalo";
            for (var admin : usuarioRepository.findByRol_NombreIgnoreCase("ADMIN")) {
                notificacionService.crear(admin, "PAGO", texto, "/admin/comisiones", p.getIdPago());
            }
        } catch (Exception ex) {
            log.warn("No se pudo avisar del pago {}: {}", id, ex.getMessage());
        }
        return guardado;
    }

    /** El admin confirma que el dinero llegó. */
    @Transactional
    public Pago aprobarManual(Long id) {
        Pago p = pagoRepository.findById(id).orElseThrow(() -> new IllegalStateException("Pago no encontrado"));
        if (!"EN_REVISION".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Este pago no está en revisión");
        }
        aplicarComision(p);
        p.setEstado("PAGADO");
        p.setNotaRevision(null);
        p.setFechaPago(java.time.LocalDateTime.now());
        if (p.getServicio() != null) {
            p.getServicio().setMonto(p.getMonto());
        }
        Pago guardado = pagoRepository.save(p);
        notificarPagado(guardado);
        return guardado;
    }

    /** El admin no encontró el dinero: el pago vuelve a PENDIENTE con un motivo para que el cliente lo corrija. */
    @Transactional
    public Pago rechazarManual(Long id, String motivo) {
        Pago p = pagoRepository.findById(id).orElseThrow(() -> new IllegalStateException("Pago no encontrado"));
        if (!"EN_REVISION".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Este pago no está en revisión");
        }
        String nota = (motivo == null || motivo.isBlank()) ? "No pudimos verificar la operación" : motivo.trim();
        if (nota.length() > 200) nota = nota.substring(0, 200);
        p.setEstado("PENDIENTE");
        p.setNotaRevision(nota);
        p.setCodigoOperacion(null);
        p.setReferenciaPago(null);
        Pago guardado = pagoRepository.save(p);
        try {
            if (p.getCliente() != null) {
                notificacionService.crear(p.getCliente().getUsuario(), "PAGO",
                        "No pudimos verificar tu pago: " + nota + ". Revisa y vuelve a intentarlo",
                        "/pagos?pagoId=" + p.getIdPago(), p.getIdPago());
            }
        } catch (Exception ex) {
            log.warn("No se pudo avisar al cliente del rechazo del pago {}: {}", id, ex.getMessage());
        }
        return guardado;
    }

    /** Marca que ya se liquidó: transferido al técnico (Yape/Plin) o comisión cobrada (efectivo). */
    @Transactional
    public Pago marcarLiquidado(Long id) {
        Pago p = pagoRepository.findById(id).orElseThrow(() -> new IllegalStateException("Pago no encontrado"));
        if (!"PAGADO".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Solo se liquidan pagos ya pagados");
        }
        p.setLiquidado(true);
        p.setFechaLiquidacion(java.time.LocalDateTime.now());
        Pago guardado = pagoRepository.save(p);
        try {
            var tecnico = (p.getServicio() != null) ? p.getServicio().getTecnico() : null;
            if (!"efectivo".equalsIgnoreCase(p.getMetodoPago()) && tecnico != null && tecnico.getUsuario() != null) {
                BigDecimal neto = p.getMontoTecnico() != null ? p.getMontoTecnico()
                        : p.getMonto().subtract(calcularComision(p.getMonto()));
                notificacionService.crear(tecnico.getUsuario(), "PAGO",
                        "HogarFix te transfirió S/ " + neto + " por tu servicio", "/tecnicos/panel", p.getIdPago());
            }
        } catch (Exception ex) {
            log.warn("No se pudo avisar la transferencia del pago {}: {}", id, ex.getMessage());
        }
        return guardado;
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

    /** Manda la boleta del servicio al correo con el que se registró el cliente. No falla si el correo falla. */
    public void enviarBoleta(Pago p) {
        try {
            if (p.getCliente() == null || p.getCliente().getUsuario() == null || p.getServicio() == null) return;
            String email = p.getCliente().getUsuario().getEmail();
            var s = p.getServicio();
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String nombreCliente = p.getCliente().getNombres();
            String tecnico = s.getTecnico() != null
                    ? (s.getTecnico().getNombres() + " " + s.getTecnico().getApellidoPaterno()).trim() : "-";
            emailService.sendBoletaEmail(email, nombreCliente, s.getIdServicio(),
                    String.format("B001-%06d", p.getIdPago()),
                    s.getFechaFinalizacion() != null ? s.getFechaFinalizacion().format(fmt) : "-",
                    p.getFechaPago() != null ? p.getFechaPago().format(fmt) : "-",
                    tecnico,
                    s.getCategoria() != null ? s.getCategoria().getNombre() : "Servicio",
                    s.getDescripcion(),
                    metodoTexto(p.getMetodoPago()),
                    p.getMonto().setScale(2, RoundingMode.HALF_UP).toPlainString());
        } catch (Exception ex) {
            log.warn("No se pudo enviar la boleta del pago {}: {}", p.getIdPago(), ex.getMessage());
        }
    }

    private void notificarPagado(Pago p) {
        try {
            BigDecimal comision = p.getComision() != null ? p.getComision() : calcularComision(p.getMonto());
            BigDecimal neto = p.getMontoTecnico() != null ? p.getMontoTecnico() : p.getMonto().subtract(comision);
            var tecnico = (p.getServicio() != null) ? p.getServicio().getTecnico() : null;
            boolean efectivo = "efectivo".equalsIgnoreCase(p.getMetodoPago());

            if (tecnico != null && tecnico.getUsuario() != null) {
                String msg = efectivo
                        ? "El cliente pagó S/ " + p.getMonto() + " en efectivo. Entrega a HogarFix la comisión de S/ "
                                + comision + " (te quedan S/ " + neto + ")"
                        : "El cliente pagó S/ " + p.getMonto() + " a HogarFix. Te transferiremos S/ " + neto
                                + " (comisión S/ " + comision + ")";
                notificacionService.crear(tecnico.getUsuario(), "PAGO", msg, "/tecnicos/panel", p.getIdPago());
            }
            enviarBoleta(p);
            if (!efectivo && p.getCliente() != null) {
                notificacionService.crear(p.getCliente().getUsuario(), "PAGO",
                        "Tu pago de S/ " + p.getMonto() + " fue confirmado. Ya puedes ver tu boleta",
                        p.getServicio() != null ? "/boletas/" + p.getServicio().getIdServicio() : "/clientes/perfil",
                        p.getIdPago());
            }
        } catch (Exception ex) {
            log.warn("No se pudo notificar el pago {}: {}", p.getIdPago(), ex.getMessage());
        }
    }
}
