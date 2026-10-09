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

    /** Porcentaje que se queda HogarFix por servicio (variable de entorno COMISION_PORCENTAJE, por defecto 10). */
    @Value("${COMISION_PORCENTAJE:10}")
    private BigDecimal porcentajeComision;

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
            for (var admin : usuarioRepository.findByRol_NombreIgnoreCase("ADMIN")) {
                notificacionService.crear(admin, "PAGO",
                        cliente + " informó un pago por " + m.toUpperCase() + " de S/ " + p.getMonto()
                                + " (operación " + c + ")",
                        "/admin/comisiones", p.getIdPago());
            }
        } catch (Exception ex) {
            log.warn("No se pudo avisar a los administradores del pago {}: {}", id, ex.getMessage());
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

    /** Marca que ya se liquidó: transferido al técnico (pagos online) o comisión cobrada (efectivo). */
    @Transactional
    public Pago marcarLiquidado(Long id) {
        Pago p = pagoRepository.findById(id).orElseThrow(() -> new IllegalStateException("Pago no encontrado"));
        if (!"PAGADO".equalsIgnoreCase(p.getEstado())) {
            throw new IllegalStateException("Solo se liquidan pagos ya pagados");
        }
        p.setLiquidado(true);
        p.setFechaLiquidacion(java.time.LocalDateTime.now());
        return pagoRepository.save(p);
    }

    private void notificarPagado(Pago p) {
        try {
            BigDecimal comision = p.getComision() != null ? p.getComision() : calcularComision(p.getMonto());
            BigDecimal neto = p.getMontoTecnico() != null ? p.getMontoTecnico() : p.getMonto().subtract(comision);
            var tecnico = (p.getServicio() != null) ? p.getServicio().getTecnico() : null;
            boolean efectivo = "efectivo".equalsIgnoreCase(p.getMetodoPago());

            if (tecnico != null && tecnico.getUsuario() != null) {
                String msg = efectivo
                        ? "El cliente confirmó el pago en efectivo de S/ " + p.getMonto()
                                + ". Recuerda entregar a HogarFix la comisión de S/ " + comision
                        : "Pago recibido: S/ " + p.getMonto() + ". Tu parte es S/ " + neto
                                + " (comisión HogarFix S/ " + comision + ")";
                notificacionService.crear(tecnico.getUsuario(), "PAGO", msg, "/tecnicos/panel", p.getIdPago());
            }
            if (!"efectivo".equalsIgnoreCase(p.getMetodoPago()) && p.getCliente() != null) {
                notificacionService.crear(p.getCliente().getUsuario(), "PAGO",
                        "Tu pago de S/ " + p.getMonto() + " fue confirmado. ¡Gracias!", "/clientes/perfil",
                        p.getIdPago());
            }
        } catch (Exception ex) {
            log.warn("No se pudo notificar el pago {}: {}", p.getIdPago(), ex.getMessage());
        }
    }
}
