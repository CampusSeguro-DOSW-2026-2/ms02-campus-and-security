package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacioRepository extends JpaRepository<Espacio, UUID> {

    List<Espacio> findByEdificioId(UUID edificioId);

    List<Espacio> findByPisoId(UUID pisoId);

    boolean existsByEdificioIdAndCodigo(UUID edificioId, String codigo);

    boolean existsByEdificioIdAndCodigoAndIdNot(UUID edificioId, String codigo, UUID id);
}
