package com.hogarfix.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hogarfix.model.Comentario;
import com.hogarfix.model.Servicio;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByServicio(Servicio servicio);
}
