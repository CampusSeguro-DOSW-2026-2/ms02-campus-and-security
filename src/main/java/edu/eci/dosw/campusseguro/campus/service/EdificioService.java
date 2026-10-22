package edu.eci.dosw.campusseguro.campus.service;

import edu.eci.dosw.campusseguro.campus.dto.EdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EdificioResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EdificioService {

    private final EdificioRepository edificioRepository;

    @Transactional
    public EdificioResponse crear(EdificioRequest request) {
        String codigo = request.codigo().trim();
        if (edificioRepository.existsByCodigo(codigo)) {
            throw new CodigoDuplicadoException(codigo);
        }
        Edificio edificio = new Edificio();
        edificio.setCodigo(codigo);
        edificio.setNombre(request.nombre().trim());
        edificio.setDescripcion(request.descripcion());
        return aRespuesta(edificioRepository.save(edificio));
    }

    @Transactional
    public EdificioResponse modificar(Long id, EdificioRequest request) {
        Edificio edificio = buscar(id);
        String codigo = request.codigo().trim();
        if (edificioRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new CodigoDuplicadoException(codigo);
        }
        edificio.setCodigo(codigo);
        edificio.setNombre(request.nombre().trim());
        edificio.setDescripcion(request.descripcion());
        return aRespuesta(edificioRepository.save(edificio));
    }

    @Transactional
    public EdificioResponse inactivar(Long id) {
        Edificio edificio = buscar(id);
        edificio.setEstado(Estado.INACTIVO);
        return aRespuesta(edificioRepository.save(edificio));
    }

    @Transactional(readOnly = true)
    public List<EdificioResponse> listar() {
        return edificioRepository.findAll().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EdificioResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    private Edificio buscar(Long id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private EdificioResponse aRespuesta(Edificio e) {
        return new EdificioResponse(
                e.getId(), e.getCodigo(), e.getNombre(), e.getDescripcion(), e.getEstado());
    }
}
