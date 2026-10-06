package com.hogarfix.repository;

import com.hogarfix.model.Pago;
import com.hogarfix.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByServicio(Servicio servicio);
    java.util.List<Pago> findByCliente(com.hogarfix.model.Cliente cliente);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.servicio.tecnico.idTecnico = :idTecnico AND p.estado = 'PAGADO' " +
            "AND p.fechaPago >= :desde")
    java.math.BigDecimal sumMontoPagadoPorTecnicoDesde(@Param("idTecnico") Long idTecnico,
            @Param("desde") LocalDateTime desde);
}