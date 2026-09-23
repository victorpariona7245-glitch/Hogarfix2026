package com.hogarfix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hogarfix.model.Direccion;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {
}
