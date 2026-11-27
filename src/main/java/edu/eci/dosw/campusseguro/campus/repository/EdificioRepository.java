package edu.eci.dosw.campusseguro.campus.repository;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdificioRepository extends JpaRepository<Edificio, UUID> {


    boolean existsByCampusIdAndEstado(UUID campusId, Estado estado);
}