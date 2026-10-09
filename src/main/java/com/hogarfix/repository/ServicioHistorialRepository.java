package com.hogarfix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.ServicioHistorial;

@Repository
public interface ServicioHistorialRepository extends JpaRepository<ServicioHistorial, Long> {

    List<ServicioHistorial> findByServicio_IdServicioOrderByFechaAsc(Long idServicio);
}
