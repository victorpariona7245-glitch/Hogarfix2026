package com.hogarfix.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Problema reportado por el cliente o el técnico sobre un servicio. Mientras esté ABIERTA, el dinero queda retenido. */
@Entity
@Table(name = "tbl_disputa")
@Getter
@Setter
@NoArgsConstructor
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDisputa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idServicio", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuarioAbre", nullable = false)
    private Usuario abiertaPor;

    @Column(nullable = false, length = 10)
    private String rolAbre; // CLIENTE o TECNICO

    @Column(nullable = false, length = 40)
    private String motivo;

    @Column(nullable = false, length = 600)
    private String descripcion;

    @Column(nullable = false, length = 12)
    private String estado = "ABIERTA"; // ABIERTA, RESUELTA

    @Column(length = 20)
    private String decision; // A_FAVOR_CLIENTE, A_FAVOR_TECNICO, ACUERDO

    @Column(length = 500)
    private String resolucion;

    @Column(length = 120)
    private String resueltaPor;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaResolucion;
}
