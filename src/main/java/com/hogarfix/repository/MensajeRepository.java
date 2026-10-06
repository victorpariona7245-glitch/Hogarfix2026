package com.hogarfix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.Mensaje;
import com.hogarfix.model.Servicio;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByServicioOrderByFechaEnvioAsc(Servicio servicio);

    List<Mensaje> findByServicio_IdServicioOrderByFechaEnvioAsc(Long idServicio);
}