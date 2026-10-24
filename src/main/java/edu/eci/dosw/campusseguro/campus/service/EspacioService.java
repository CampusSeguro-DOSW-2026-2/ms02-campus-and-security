package edu.eci.dosw.campusseguro.campus.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEspacioRequest;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;

    @Transactional
    public EspacioResponse crear(UUID pisoId, EspacioRequest request) {
        Piso piso = buscarPiso(pisoId);
        Edificio edificio = buscarEdificio(piso.getEdificioId());
        if (edificio.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar espacios en un edificio inactivo");
        }
        if (piso.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar espacios en un piso inactivo");
        }
        String codigo = request.codigo().trim();
        if (espacioRepository.existsByEdificioIdAndCodigo(edificio.getId(), codigo)) {
            throw new EspacioDuplicadoException(codigo);
        }
        Espacio espacio = new Espacio();
        espacio.setEdificioId(edificio.getId());
        espacio.setPisoId(pisoId);
        espacio.setCodigo(codigo);
        espacio.setNombre(request.nombre().trim());
        espacio.setTipo(request.tipo());
        return aRespuesta(espacioRepository.save(espacio));
    }

    @Transactional
    public EspacioResponse modificar(UUID id, ModificarEspacioRequest request) {
        Espacio espacio = buscar(id);
        espacio.setNombre(request.nombre().trim());
        espacio.setTipo(request.tipo());
        return aRespuesta(espacioRepository.save(espacio));
    }

    @Transactional(readOnly = true)
    public List<EspacioResponse> listarPorPiso(UUID pisoId) {
        buscarPiso(pisoId);
        return espacioRepository.findByPisoId(pisoId).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EspacioResponse> listarPorEdificio(UUID edificioId) {
        buscarEdificio(edificioId);
        return espacioRepository.findByEdificioId(edificioId).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EspacioResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private Espacio buscar(UUID id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException(id));
    }

    private Piso buscarPiso(UUID id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new PisoNoEncontradoException(id));
    }

    private Edificio buscarEdificio(UUID id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private EspacioResponse aRespuesta(Espacio e) {
        return new EspacioResponse(e.getId(), e.getEdificioId(), e.getPisoId(), e.getCodigo(),
                e.getNombre(), e.getTipo(), e.getEstado());
    }
}
