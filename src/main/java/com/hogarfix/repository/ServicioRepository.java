package com.hogarfix.repository;

import com.hogarfix.model.Cliente;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findByCliente(Cliente cliente);

    List<Servicio> findByTecnico(Tecnico tecnico);

    List<Servicio> findByTecnico_IdTecnico(Long idTecnico);

    List<Servicio> findByEstadoAndFechaSolicitudBefore(String estado, LocalDateTime limite);
}