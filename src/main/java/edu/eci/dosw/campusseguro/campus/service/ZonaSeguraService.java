package edu.eci.dosw.campusseguro.campus.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
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
    public ZonaSeguraResponse modificar(UUID id, ZonaSeguraRequest request) {
        ZonaSegura zona = buscar(id);
        copiarDatos(zona, request);
        return aRespuesta(zonaSeguraRepository.save(zona));
    }

    @Transactional
    public ZonaSeguraResponse inactivar(UUID id) {
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
    public ZonaSeguraResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private ZonaSegura buscar(UUID id) {
        return zonaSeguraRepository.findById(id)
                .orElseThrow(() -> new ZonaSeguraNoEncontradaException(id));
    }

    private void copiarDatos(ZonaSegura zona, ZonaSeguraRequest request) {
        zona.setCodigo(request.codigo().trim());
        zona.setNombre(request.nombre().trim());
        zona.setUbicacion(request.ubicacion().trim());
        zona.setDescripcion(request.descripcion().trim());
        zona.setCapacidad(request.capacidad());
    }
    private ZonaSeguraResponse aRespuesta(ZonaSegura z) {
        return new ZonaSeguraResponse(
                z.getId(), z.getCodigo(), z.getNombre(), z.getUbicacion(),
                z.getDescripcion(), z.getCapacidad(), z.getEstado());
    }
}