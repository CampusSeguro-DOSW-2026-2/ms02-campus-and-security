package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Escalera;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscaleraRepository extends JpaRepository<Escalera, UUID> {

    List<Escalera> findByEdificioId(UUID edificioId);

    boolean existsByCodigo(String codigo);
    List<Escalera> findByPisos_IdAndEstado(UUID pisoId, Estado estado);
}
