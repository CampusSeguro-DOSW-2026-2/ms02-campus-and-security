package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class VerificadorRutasService implements VerificadorRutas {

    private static final Set<EstadoRuta> ACTIVAS = EnumSet.of(EstadoRuta.DISPONIBLE, EstadoRuta.RESTRINGIDA);

    private final RutaRepository rutaRepository;
    private final EspacioGateway espacioGateway;

    public VerificadorRutasService(RutaRepository rutaRepository, EspacioGateway espacioGateway) {
        this.rutaRepository = rutaRepository;
        this.espacioGateway = espacioGateway;
    }

    @Override
    public List<UUID> rutasActivasDeEspacio(UUID espacioId) {
        return ids(rutaRepository.findByEspacioOrigenIdAndEstadoIn(espacioId, ACTIVAS));
    }

    @Override
    public List<UUID> rutasActivasDeZonaSegura(UUID zonaSeguraId) {
        return ids(rutaRepository.findByZonaSeguraDestinoIdAndEstadoIn(zonaSeguraId, ACTIVAS));
    }

    @Override
    public List<UUID> rutasActivasDeEdificio(UUID edificioId) {
        List<UUID> espacios = espacioGateway.listarPorEdificio(edificioId).stream()
                .filter(EspacioInfo::disponible)
                .map(EspacioInfo::id)
                .toList();
        if (espacios.isEmpty()) {
            return List.of();
        }
        return ids(rutaRepository.findByEspacioOrigenIdInAndEstadoIn(espacios, ACTIVAS));
    }

    private static List<UUID> ids(List<RutaEvacuacion> rutas) {
        return rutas.stream().map(RutaEvacuacion::getId).toList();
    }
}
