package com.hogarfix.repository;

import java.util.List;
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
}