package com.hogarfix.model;

import java.time.LocalDateTime;

import com.hogarfix.model.base.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_comentario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comentario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idComentario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idServicio", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 500)
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime fechaComentario;
}
