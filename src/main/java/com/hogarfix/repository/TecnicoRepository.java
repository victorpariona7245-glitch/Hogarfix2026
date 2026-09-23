package com.hogarfix.repository;

import com.hogarfix.model.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// Aplicación del principio DAO (Objeto de Acceso a Datos)
// Esta interfaz define las operaciones CRUD básicas sin necesidad de código SQL.

@Repository
public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    @Query("""
                SELECT DISTINCT t
                FROM Tecnico t
                JOIN t.categorias tc
                JOIN tc.categoria c
                WHERE LOWER(c.nombre) = LOWER(:nombreCategoria)
            """)
    List<Tecnico> findTecnicosByCategoriaNombre(@Param("nombreCategoria") String nombreCategoria);

    // Verificar si un email ya existe
    boolean existsByUsuario_Email(String email);

    // Verificar si un dni ya existe
    boolean existsByDni(String dni);

    Optional<Tecnico> findByDni(String dni);

    Optional<Tecnico> findByUsuario_Email(String email);
}
