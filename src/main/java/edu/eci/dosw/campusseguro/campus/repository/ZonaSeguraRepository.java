package edu.eci.dosw.campusseguro.campus.repository;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaSeguraRepository extends JpaRepository<ZonaSegura, UUID> {

    boolean existsByCodigo(String codigo);

    boolean existsByZonaAbiertaIdAndEstado(UUID zonaAbiertaId, Estado estado);

}