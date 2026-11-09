package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Salida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalidaRepository extends JpaRepository<Salida, UUID> {

    List<Salida> findByEdificioIdOrderByCodigoAsc(UUID edificioId);

    boolean existsByEdificioIdAndCodigo(UUID edificioId, String codigo);

    List<Salida> findByPisoId(UUID pisoId);
}
