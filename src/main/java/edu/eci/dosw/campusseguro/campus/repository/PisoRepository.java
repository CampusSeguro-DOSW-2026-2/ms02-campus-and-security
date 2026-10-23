package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Piso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PisoRepository extends JpaRepository<Piso, UUID> {

    List<Piso> findByEdificioIdOrderByNumeroAsc(UUID edificioId);

    boolean existsByEdificioIdAndNumero(UUID edificioId, Integer numero);

    boolean existsByEdificioIdAndNumeroAndIdNot(UUID edificioId, Integer numero, UUID id);
}
