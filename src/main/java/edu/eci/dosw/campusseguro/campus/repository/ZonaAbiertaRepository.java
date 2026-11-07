package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaAbierta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaAbiertaRepository extends JpaRepository<ZonaAbierta, UUID> {

    List<ZonaAbierta> findByCampusIdOrderByNombreAsc(UUID campusId);

    boolean existsByCampusIdAndEstado(UUID campusId, Estado estado);
}
