package com.hogarfix.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aviso dentro de la web (campanita) dirigido a un usuario. */
@Entity
@Table(name = "tbl_notificacion", indexes = @Index(name = "idx_notif_usuario", columnList = "idUsuario, leida"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String tipo; // SOLICITUD, ESTADO, MENSAJE, CALIFICACION

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Column(length = 200)
    private String enlace;

    private Long idReferencia; // id del servicio relacionado (opcional)

    @Builder.Default
    @Column(nullable = false)
    private boolean leida = false;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
