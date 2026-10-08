package com.hogarfix.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una línea del registro de actividad del panel de administración. */
@Entity
@Table(name = "tbl_registro_actividad")
@Getter
@Setter
@NoArgsConstructor
public class RegistroActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Quién lo hizo (correo o usuario del administrador). */
    @Column(nullable = false, length = 150)
    private String administrador;

    /** Qué tipo de acción fue, por ejemplo "Especialidad creada". */
    @Column(nullable = false, length = 100)
    private String accion;

    @Column(nullable = false, length = 500)
    private String detalle;
}
