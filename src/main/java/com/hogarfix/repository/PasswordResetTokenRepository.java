package com.hogarfix.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hogarfix.model.PasswordResetToken;
import com.hogarfix.model.Usuario;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.usuario WHERE t.tokenHash = :hash")
    Optional<PasswordResetToken> findByTokenHash(@Param("hash") String hash);

    boolean existsByUsuarioAndCreadoEnAfter(Usuario usuario, LocalDateTime desde);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.usuario = :usuario")
    void borrarDeUsuario(@Param("usuario") Usuario usuario);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiraEn < :limite")
    int borrarExpirados(@Param("limite") LocalDateTime limite);
}
