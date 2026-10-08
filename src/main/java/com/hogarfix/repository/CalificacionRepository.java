package com.hogarfix.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.hogarfix.model.Calificacion;
import com.hogarfix.model.Servicio;
import com.hogarfix.model.Tecnico;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    List<Calificacion> findByServicio(Servicio servicio);

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.servicio.tecnico = :tecnico")
    Double promedioPorTecnico(@Param("tecnico") Tecnico tecnico);

    /** Reseñas más recientes de un técnico (con el servicio y el cliente ya cargados). */
    @Query("SELECT c FROM Calificacion c JOIN FETCH c.servicio s JOIN FETCH s.cliente "
            + "WHERE s.tecnico.idTecnico = :idTecnico ORDER BY c.fechaCalificacion DESC")
    List<Calificacion> findResenasDeTecnico(@Param("idTecnico") Long idTecnico, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Calificacion c WHERE c.servicio.tecnico.idTecnico = :idTecnico")
    long contarDeTecnico(@Param("idTecnico") Long idTecnico);

    /** Cantidad de calificaciones por técnico: filas [idTecnico, cantidad]. */
    @Query("SELECT c.servicio.tecnico.idTecnico, COUNT(c) FROM Calificacion c "
            + "WHERE c.servicio.tecnico IS NOT NULL GROUP BY c.servicio.tecnico.idTecnico")
    List<Object[]> contarPorTecnico();
}
