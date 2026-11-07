package edu.eci.dosw.campusseguro.campus.repository;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, UUID> {
}
