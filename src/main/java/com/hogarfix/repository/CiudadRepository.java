package com.hogarfix.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hogarfix.model.Ciudad;

@Repository
public interface CiudadRepository extends JpaRepository<Ciudad, Long> {
    List<Ciudad> findByPaisIdPais(Long idPais);
}
