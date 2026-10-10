package com.hogarfix.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Material comprado por el técnico para un servicio, con su comprobante (captura o foto). */
@Entity
@Table(name = "tbl_servicio_material")
@Getter
@Setter
@NoArgsConstructor
public class ServicioMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMaterial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idServicio", nullable = false)
    private Servicio servicio;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    // La imagen va en la base de datos (el disco de Railway se borra en cada despliegue)
    @JsonIgnore
    @Lob
    @Column(name = "comprobante", columnDefinition = "MEDIUMBLOB")
    private byte[] comprobante;
}
