package com.hogarfix.service;

import com.hogarfix.model.Pago;
import com.hogarfix.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;

    public Pago registrarPago(Pago pago) {
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

    public java.util.Optional<Pago> marcarPagado(Long id) {
        return pagoRepository.findById(id).map(p -> {
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
    public java.util.Optional<Pago> marcarPagadoConReferencia(Long id, String metodoPago, String referenciaCulqi) {
        return pagoRepository.findById(id).map(p -> {
            p.setEstado("PAGADO");
            p.setFechaPago(java.time.LocalDateTime.now());
            p.setMetodoPago(metodoPago);
            p.setReferenciaPago(referenciaCulqi);
            try {
                if (p.getServicio() != null) {
                    p.getServicio().setMonto(p.getMonto());
                }
            } catch (Exception ignored) {}
            return pagoRepository.save(p);
        });
    }
}