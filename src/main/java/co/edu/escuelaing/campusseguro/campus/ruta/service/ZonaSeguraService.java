package co.edu.escuelaing.campusseguro.campus.service;

import co.edu.escuelaing.campusseguro.campus.dto.ZonaSeguraRequest;
import co.edu.escuelaing.campusseguro.campus.dto.ZonaSeguraResponse;
import co.edu.escuelaing.campusseguro.campus.entity.Estado;
import co.edu.escuelaing.campusseguro.campus.entity.ZonaSegura;
import co.edu.escuelaing.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import co.edu.escuelaing.campusseguro.campus.repository.ZonaSeguraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaSeguraService {

    private final ZonaSeguraRepository zonaSeguraRepository;

    @Transactional
    public ZonaSeguraResponse crear(ZonaSeguraRequest request) {
        ZonaSegura zona = new ZonaSegura();
        copiarDatos(zona, request);
        return aRespuesta(zonaSeguraRepository.save(zona));
    }

    @Transactional
    public ZonaSeguraResponse modificar(Long id, ZonaSeguraRequest request) {
        ZonaSegura zona = buscar(id);
        copiarDatos(zona, request);
        return aRespuesta(zonaSeguraRepository.save(zona));
    }

    @Transactional
    public ZonaSeguraResponse inactivar(Long id) {
        ZonaSegura zona = buscar(id);
        zona.setEstado(Estado.INACTIVO);
        return aRespuesta(zonaSeguraRepository.save(zona));
    }

    @Transactional(readOnly = true)
    public List<ZonaSeguraResponse> listar() {
        return zonaSeguraRepository.findAll().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ZonaSeguraResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    private ZonaSegura buscar(Long id) {
        return zonaSeguraRepository.findById(id)
                .orElseThrow(() -> new ZonaSeguraNoEncontradaException(id));
    }

    private void copiarDatos(ZonaSegura zona, ZonaSeguraRequest request) {
        zona.setNombre(request.nombre().trim());
        zona.setUbicacion(request.ubicacion().trim());
        zona.setDescripcion(request.descripcion().trim());
        zona.setCapacidad(request.capacidad());
    }

    private ZonaSeguraResponse aRespuesta(ZonaSegura z) {
        return new ZonaSeguraResponse(
                z.getId(), z.getNombre(), z.getUbicacion(),
                z.getDescripcion(), z.getCapacidad(), z.getEstado());
    }
}