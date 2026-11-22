
package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-MS02-05: ya no se crean rutas manuales espacio -> zona, así que lo único que puede estar
// "activo" para un espacio es una ruta exterior (salida -> zona) alcanzable desde el edificio donde
// está ese espacio. No se filtra por piso/escalera (ese cálculo es de la guía automática, en otro
// servicio): si el edificio tiene alguna ruta exterior activa, se bloquea la desactivación de
// cualquiera de sus espacios. Es una aproximación conservadora a propósito, para un guardia de
// seguridad: prefiere bloquear de más a dejar pasar una desactivación que debía evitarse.
@Service
@Transactional(readOnly = true)
public class VerificadorRutasService implements VerificadorRutas {

    private static final Set<EstadoRuta> ACTIVAS =
            EnumSet.of(EstadoRuta.DISPONIBLE, EstadoRuta.RESTRINGIDA);

    private final RutaRepository rutaRepository;
    private final EspacioGateway espacioGateway;
    private final SalidaGateway salidaGateway;

    public VerificadorRutasService(
            RutaRepository rutaRepository,
            EspacioGateway espacioGateway,
            SalidaGateway salidaGateway) {
        this.rutaRepository = rutaRepository;
        this.espacioGateway = espacioGateway;
        this.salidaGateway = salidaGateway;
    }

    @Override
    public List<UUID> rutasActivasDeEspacio(UUID espacioId) {
        List<UUID> legado = ids(
                rutaRepository.findByEspacioOrigenIdAndEstadoIn(
                        espacioId, ACTIVAS));
        if (!legado.isEmpty()) {
            return legado;
        }

        UUID edificioId = espacioGateway.buscarPorId(espacioId)
                .map(EspacioGateway.EspacioInfo::edificioId)
                .orElse(null);
        return edificioId == null ? List.of() : rutasExterioresActivasDelEdificio(edificioId);
    }

    @Override
    public List<UUID> rutasActivasDeZonaSegura(UUID zonaSeguraId) {
        // Por el id de la zona de destino, no por el origen: sirve igual para rutas de legado y
        // exteriores, porque ambas guardan la zona en la misma columna.
        return ids(
                rutaRepository.findByZonaSeguraDestinoIdAndEstadoIn(
                        zonaSeguraId, ACTIVAS));
    }

    @Override
    public List<UUID> rutasActivasDeEdificio(UUID edificioId) {
        List<UUID> espacios = espacioGateway.listarPorEdificio(edificioId)
                .stream()
                .map(EspacioGateway.EspacioInfo::id)
                .toList();

        List<UUID> legado = espacios.isEmpty() ? List.of()
                : ids(rutaRepository.findByEspacioOrigenIdInAndEstadoIn(espacios, ACTIVAS));
        if (!legado.isEmpty()) {
            return legado;
        }

        return rutasExterioresActivasDelEdificio(edificioId);
    }

    private List<UUID> rutasExterioresActivasDelEdificio(UUID edificioId) {
        List<UUID> salidas = salidaGateway.listarPorEdificio(edificioId).stream()
                .map(SalidaGateway.SalidaInfo::id)
                .toList();
        if (salidas.isEmpty()) {
            return List.of();
        }
        return ids(rutaRepository.findBySalidaOrigenIdInAndEstadoIn(salidas, ACTIVAS));
    }

    private static List<UUID> ids(List<RutaEvacuacion> rutas) {
        return rutas.stream()
                .map(RutaEvacuacion::getId)
                .toList();
    }
}
