package com.hogarfix.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import com.hogarfix.model.base.Auditable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "idTecnico", nullable = true)
    private Tecnico tecnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCategoria", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "direccion_servicio", length = 255)
    private String direccionServicio;

    @Column(length = 20)
    private String urgencia;

    @Column(name = "metodo_pago_preferido", length = 20)
    private String metodoPagoPreferido; // "tarjeta" o "efectivo" — elegido por el cliente al solicitar

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false, length = 30)
    private String estado; // ejemplo: "PENDIENTE", "EN_PROCESO", "FINALIZADO"

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaFinalizacion;

    @OneToOne(mappedBy = "servicio", cascade = CascadeType.ALL)
    private Pago pago;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Calificacion> calificaciones;

    @Builder.Default
    @Column(name = "num_solicitudes")
    private Integer numSolicitudes = 0;
}