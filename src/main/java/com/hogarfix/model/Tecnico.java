package com.hogarfix.model;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

import java.util.ArrayList;
import java.util.List;

import com.hogarfix.model.base.Auditable;

@Entity
@Table(name = "tbl_tecnico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tecnico extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTecnico;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidoPaterno;

    @Column(length = 100)
    private String apellidoMaterno;

    @Column(nullable = false, length = 20, unique = true)
    private String dni;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 255)
    private String certificadoPdf; // ruta o nombre del archivo del certificado

    @Column(length = 255)
    private String fotoPerfil; // ruta relativa a uploads/tecnicos/...

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "idDireccion", nullable = false)
    private Direccion direccion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Builder.Default
    @OneToMany(mappedBy = "tecnico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TecnicoCategoria> categorias = new ArrayList<>();

    @Builder.Default
    @Column(name = "promedio_calificacion")
    private Double promedioCalificacion = 0.0;

    @Builder.Default
    @Column(name = "disponible")
    private Boolean disponible = true; // el técnico puede marcarse disponible/no disponible
}