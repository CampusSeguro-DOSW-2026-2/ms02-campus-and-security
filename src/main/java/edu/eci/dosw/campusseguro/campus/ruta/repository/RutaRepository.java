package edu.eci.dosw.campusseguro.campus.ruta.repository;

import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RutaRepository extends JpaRepository<RutaEvacuacion, Long> {

    boolean existsByEspacioOrigenId(Long espacioId);

    boolean existsByZonaSeguraDestinoId(Long zonaSeguraId);

    boolean existsByEspacioOrigenIdIn(Collection<Long> espacioIds);

    Optional<RutaEvacuacion> findByEspacioOrigenId(Long espacioId);

    List<RutaEvacuacion> findByEspacioOrigenIdIn(Collection<Long> espacioIds);
}
