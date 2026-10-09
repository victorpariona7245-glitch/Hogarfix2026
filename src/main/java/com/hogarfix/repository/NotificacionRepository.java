package com.hogarfix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.Notificacion;
import com.hogarfix.model.Usuario;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop15ByUsuarioOrderByFechaDesc(Usuario usuario);

    long countByUsuarioAndLeidaFalse(Usuario usuario);

    boolean existsByUsuarioAndTipoAndIdReferenciaAndLeidaFalse(Usuario usuario, String tipo, Long idReferencia);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario = :usuario AND n.leida = false")
    int marcarTodasLeidas(@Param("usuario") Usuario usuario);

    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.fecha < :limite")
    int borrarAnterioresA(@Param("limite") java.time.LocalDateTime limite);
}
