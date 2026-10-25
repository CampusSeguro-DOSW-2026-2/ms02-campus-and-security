package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.entity.RecomendacionPreventiva;

public interface RecomendacionPreventivaRepository
        extends JpaRepository<RecomendacionPreventiva, UUID> {

    List<RecomendacionPreventiva> findByEstado(Estado estado);

    List<RecomendacionPreventiva> findByEstadoAndEdificioId(
            Estado estado, UUID edificioId);

    List<RecomendacionPreventiva> findByEstadoAndTipoEspacio(
            Estado estado, TipoEspacio tipoEspacio);
}
