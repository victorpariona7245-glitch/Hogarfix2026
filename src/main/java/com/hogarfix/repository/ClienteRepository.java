package com.hogarfix.repository;

import com.hogarfix.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Método de soporte para la lógica de negocio (ej. evitar registro con email
    // duplicado)
    boolean existsByUsuario_Email(String email);

    boolean existsByDni(String dni);

    // Método para la autenticación simple
    Optional<Cliente> findByUsuario_Email(String email);

    Optional<Cliente> findByUsuario_Username(String username);

    Optional<Cliente> findByUsuario_EmailAndUsuario_Password(String email, String password);
}
