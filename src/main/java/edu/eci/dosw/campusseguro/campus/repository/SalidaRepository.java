package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Salida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalidaRepository extends JpaRepository<Salida, UUID> {

    List<Salida> findByEdificioIdOrderByNombreAsc(UUID edificioId);


    List<Salida> findByPisoId(UUID pisoId);

    boolean existsByPisoIdAndEstado(UUID pisoId, edu.eci.dosw.campusseguro.campus.entity.Estado estado);
}
