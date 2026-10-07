package com.hogarfix.repository;

import com.hogarfix.model.Cliente;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findByCliente(Cliente cliente);

    List<Servicio> findByTecnico(Tecnico tecnico);

    List<Servicio> findByTecnico_IdTecnico(Long idTecnico);

    List<Servicio> findByEstadoAndFechaSolicitudBefore(String estado, LocalDateTime limite);

    @Query("SELECT s FROM Servicio s JOIN FETCH s.categoria LEFT JOIN FETCH s.tecnico " +
            "WHERE s.fechaSolicitud >= :desde AND s.fechaSolicitud < :hasta")
    List<Servicio> findDelPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
