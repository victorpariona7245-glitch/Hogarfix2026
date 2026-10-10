package com.hogarfix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.Disputa;

@Repository
public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    boolean existsByServicio_IdServicioAndEstado(Long idServicio, String estado);

    long countByEstado(String estado);

    List<Disputa> findAllByOrderByFechaCreacionDesc();
}
