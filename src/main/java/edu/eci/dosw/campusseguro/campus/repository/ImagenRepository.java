package edu.eci.dosw.campusseguro.campus.repository;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Imagen;
import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenRepository extends JpaRepository<Imagen, UUID> {

    boolean existsByTipoElementoAndElementoId(
            TipoElementoImagen tipoElemento,
            UUID elementoId
    );

    List<Imagen> findAllByTipoElementoAndElementoId(
            TipoElementoImagen tipoElemento,
            UUID elementoId
    );
}
