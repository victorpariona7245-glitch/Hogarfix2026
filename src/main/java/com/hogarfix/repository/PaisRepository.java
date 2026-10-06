package com.hogarfix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hogarfix.model.Pais;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {
}
