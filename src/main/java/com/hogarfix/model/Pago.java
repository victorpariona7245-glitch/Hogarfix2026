package com.hogarfix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hogarfix.model.base.Auditable;

@Entity
@Table(name = "tbl_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idServicio", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 30)
    private String metodoPago; // tarjeta, efectivo, transferencia, etc.

    @Column(nullable = false)
    private LocalDateTime fechaPago;

    @Column(nullable = false, length = 30)
    private String estado; // PAGADO, PENDIENTE, CANCELADO

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago; // ID del cargo en la pasarela de pago (Culqi / MercadoPago)

    // --- Comisión de la plataforma ---
    @Column(precision = 10, scale = 2)
    private BigDecimal comision; // lo que se queda HogarFix

    @Column(name = "monto_tecnico", precision = 10, scale = 2)
    private BigDecimal montoTecnico; // lo que le corresponde al técnico (monto - comisión)

    // --- Pagos manuales (Yape / Plin) ---
    @Column(name = "codigo_operacion", length = 40)
    private String codigoOperacion; // nº de operación que informa el cliente

    @Column(name = "nota_revision", length = 200)
    private String notaRevision; // motivo si el admin rechaza el comprobante

    // Online: ¿ya se le transfirió al técnico su parte? Efectivo: ¿ya cobramos la comisión al técnico?
    private Boolean liquidado;

    private LocalDateTime fechaLiquidacion;
}