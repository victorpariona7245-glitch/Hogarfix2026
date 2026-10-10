package com.hogarfix.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.ServicioMaterial;

@Repository
public interface ServicioMaterialRepository extends JpaRepository<ServicioMaterial, Long> {

    /** Vista sin la imagen, para listar rápido. */
    interface MaterialResumen {
        Long getIdMaterial();

        String getDescripcion();

        BigDecimal getPrecio();
    }

    List<MaterialResumen> findByServicio_IdServicioOrderByIdMaterialAsc(Long idServicio);

    void deleteByServicio_IdServicio(Long idServicio);
}
