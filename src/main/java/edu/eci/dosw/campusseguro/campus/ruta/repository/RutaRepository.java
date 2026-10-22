package edu.eci.dosw.campusseguro.campus.ruta.repository;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RutaRepository extends JpaRepository<RutaEvacuacion, UUID> {

    boolean existsByEspacioOrigenId(UUID espacioId);

    boolean existsByZonaSeguraDestinoId(UUID zonaSeguraId);

    boolean existsByEspacioOrigenIdIn(Collection<UUID> espacioIds);

    Optional<RutaEvacuacion> findByEspacioOrigenId(UUID espacioId);

    List<RutaEvacuacion> findByEspacioOrigenIdIn(Collection<UUID> espacioIds);
}
