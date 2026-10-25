
package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.ZonaCodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ZonaSeguraService {

    private final ZonaSeguraRepository zonaSeguraRepository;
    private final VerificadorRutas verificadorRutas;

    @Transactional
    public ZonaSeguraResponse crear(ZonaSeguraRequest request) {
        String codigo = request.codigo().trim();

        if (zonaSeguraRepository.existsByCodigo(codigo)) {
            throw new ZonaCodigoDuplicadoException(codigo);
        }

        ZonaSegura zona = new ZonaSegura();
        copiarDatos(zona, request);

        return aRespuesta(zonaSeguraRepository.save(zona));
    }

    @Transactional
    public ZonaSeguraResponse modificar(UUID id, ZonaSeguraRequest request) {
        ZonaSegura zona = buscar(id);
        String codigo = request.codigo().trim();

        if (zonaSeguraRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ZonaCodigoDuplicadoException(codigo);
        }

        copiarDatos(zona, request);

        return aRespuesta(zonaSeguraRepository.save(zona));
    }

    @Transactional
    public ZonaSeguraResponse inactivar(UUID id) {
        ZonaSegura zona = buscar(id);

        List<UUID> rutasActivas =
                verificadorRutas.rutasActivasDeZonaSegura(id);

        if (!rutasActivas.isEmpty()) {
            throw new ReglaNegocioException(
                    "La zona segura es destino de rutas activas " + rutasActivas
                            + ": deben bloquearse antes de inactivarla");
        }

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
                z.getId(),
                z.getCodigo(),
                z.getNombre(),
                z.getUbicacion(),
                z.getDescripcion(),
                z.getCapacidad(),
                z.getEstado());
    }
}