package co.edu.escuelaing.campusseguro.campus.ruta.service;

import co.edu.escuelaing.campusseguro.campus.ruta.port.EspacioGateway;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.port.VerificadorRutas;
import co.edu.escuelaing.campusseguro.campus.ruta.repository.RutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VerificadorRutasService implements VerificadorRutas {

    private final RutaRepository rutaRepository;
    private final EspacioGateway espacioGateway;

    public VerificadorRutasService(RutaRepository rutaRepository, EspacioGateway espacioGateway) {
        this.rutaRepository = rutaRepository;
        this.espacioGateway = espacioGateway;
    }

    @Override
    public boolean tieneRutaEspacio(Long espacioId) {
        return rutaRepository.existsByEspacioOrigenId(espacioId);
    }

    @Override
    public boolean tieneRutaZonaSegura(Long zonaSeguraId) {
        return rutaRepository.existsByZonaSeguraDestinoId(zonaSeguraId);
    }

    @Override
    public boolean tieneRutaEdificio(Long edificioId) {
        List<Long> espacios = espacioGateway.listarPorEdificio(edificioId).stream()
                .map(EspacioInfo::id)
                .toList();
        return !espacios.isEmpty() && rutaRepository.existsByEspacioOrigenIdIn(espacios);
    }
}
