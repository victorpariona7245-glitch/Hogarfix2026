package com.hogarfix.model;

import com.hogarfix.model.base.Auditable;
import com.hogarfix.model.enums.TipoRol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRol;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    public TipoRol getTipoRol() {
        try {
            return TipoRol.valueOf(this.nombre.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}