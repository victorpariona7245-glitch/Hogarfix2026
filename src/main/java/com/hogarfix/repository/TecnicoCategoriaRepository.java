package com.hogarfix.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hogarfix.model.Categoria;
import com.hogarfix.model.Tecnico;
import com.hogarfix.model.TecnicoCategoria;

@Repository
public interface TecnicoCategoriaRepository extends JpaRepository<TecnicoCategoria, Long> {
    List<TecnicoCategoria> findByTecnico(Tecnico tecnico);

    List<TecnicoCategoria> findByCategoria(Categoria categoria);
}