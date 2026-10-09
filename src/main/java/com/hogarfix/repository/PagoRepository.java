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
    Optional<Pago> findByServicio_IdServicio(Long idServicio);
    java.util.List<Pago> findByCliente(com.hogarfix.model.Cliente cliente);

    /** Pagos ya cobrados cuya comisión el técnico todavía no entregó a HogarFix (solo pagos con comisión fijada). */
    @Query("SELECT p FROM Pago p JOIN FETCH p.servicio s WHERE s.tecnico.idTecnico = :idTecnico "
            + "AND p.estado = 'PAGADO' AND p.comision IS NOT NULL AND (p.liquidado IS NULL OR p.liquidado = false)")
    java.util.List<Pago> findComisionesPendientesDeTecnico(@Param("idTecnico") Long idTecnico);

    @Query("SELECT p FROM Pago p JOIN FETCH p.servicio s JOIN FETCH s.tecnico t "
            + "WHERE p.estado = 'PAGADO' AND p.comision IS NOT NULL AND (p.liquidado IS NULL OR p.liquidado = false)")
    java.util.List<Pago> findTodasComisionesPendientes();

    boolean existsByCodigoOperacionAndIdPagoNot(String codigoOperacion, Long idPago);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.servicio.tecnico.idTecnico = :idTecnico AND p.estado = 'PAGADO' " +
            "AND p.fechaPago >= :desde")
    java.math.BigDecimal sumMontoPagadoPorTecnicoDesde(@Param("idTecnico") Long idTecnico,
            @Param("desde") LocalDateTime desde);

    @Query("SELECT p FROM Pago p JOIN FETCH p.servicio s JOIN FETCH s.categoria " +
            "WHERE p.estado = 'PAGADO' AND p.fechaPago >= :desde AND p.fechaPago < :hasta")
    java.util.List<Pago> findPagadosDelPeriodo(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
