package com.hogarfix.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.RegistroActividad;

@Repository
public interface RegistroActividadRepository extends JpaRepository<RegistroActividad, Long> {

    /** Busca por administrador, acción o detalle (sin distinguir mayúsculas). Más recientes primero. */
    @Query("SELECT r FROM RegistroActividad r "
            + "WHERE LOWER(r.administrador) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(r.accion) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(r.detalle) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "ORDER BY r.fecha DESC, r.id DESC")
    Page<RegistroActividad> buscar(@Param("q") String q, Pageable pageable);
}
