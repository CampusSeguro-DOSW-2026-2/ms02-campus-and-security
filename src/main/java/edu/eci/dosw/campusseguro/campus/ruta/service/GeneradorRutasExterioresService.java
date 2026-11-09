package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.ResumenGeneracionResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.ruta.port.CatalogoExterior;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Genera las rutas exteriores (salida -> zona segura) sin que el administrador las cree una por una.
// Cada salida se conecta con sus N zonas seguras más cercanas; el cálculo real lo hace RutaExteriorService.
// Sin @Transactional a propósito: RutaExteriorService tampoco retiene una conexión mientras espera al proveedor.
@Service
public class GeneradorRutasExterioresService {

    private static final Logger log = LoggerFactory.getLogger(GeneradorRutasExterioresService.class);

    private record Par(UUID salidaId, UUID zonaId) {
    }

    private final CatalogoExterior catalogo;
    private final SalidaGateway salidaGateway;
    private final RutaRepository rutaRepository;
    private final RutaExteriorService rutaExteriorService;
    private final RutasAutoProperties propiedades;

    public GeneradorRutasExterioresService(CatalogoExterior catalogo,
                                           SalidaGateway salidaGateway,
                                           RutaRepository rutaRepository,
                                           RutaExteriorService rutaExteriorService,
                                           RutasAutoProperties propiedades) {
        this.catalogo = catalogo;
        this.salidaGateway = salidaGateway;
        this.rutaRepository = rutaRepository;
        this.rutaExteriorService = rutaExteriorService;
        this.propiedades = propiedades;
    }

    // Una salida nueva, movida o reactivada: calcula sus zonas más cercanas y recalcula las rutas que ya tenía.
    public ResumenGeneracionResponse generarParaSalida(UUID salidaId) {
        Optional<SalidaInfo> salida = salidaGateway.buscarPorId(salidaId).filter(SalidaInfo::disponible);
        if (salida.isEmpty()) {
            return ResumenGeneracionResponse.vacio();
        }
        Set<UUID> zonas = new LinkedHashSet<>(masCercanas(salida.get(), catalogo.zonasActivasConCoordenadas()));
        rutaRepository.findBySalidaOrigenId(salidaId).forEach(r -> zonas.add(r.getZonaSeguraDestinoId()));

        return calcular(zonas.stream().map(zona -> new Par(salidaId, zona)).toList());
    }

    // Una zona nueva, movida o reactivada: la conecta con las salidas para las que es de las más cercanas
    // y recalcula las rutas que ya llegaban a ella.
    public ResumenGeneracionResponse generarParaZona(UUID zonaId) {
        List<ZonaInfo> zonas = catalogo.zonasActivasConCoordenadas();
        if (zonas.stream().noneMatch(z -> z.id().equals(zonaId))) {
            return ResumenGeneracionResponse.vacio();
        }
        Set<UUID> salidas = new LinkedHashSet<>();
        for (SalidaInfo salida : catalogo.salidasDisponibles()) {
            if (masCercanas(salida, zonas).contains(zonaId)) {
                salidas.add(salida.id());
            }
        }
        rutaRepository.findByZonaSeguraDestinoIdAndSalidaOrigenIdNotNull(zonaId)
                .forEach(r -> salidas.add(r.getSalidaOrigenId()));

        return calcular(salidas.stream().map(salida -> new Par(salida, zonaId)).toList());
    }

    // Todo el campus de una vez. Por defecto solo crea lo que falta; con recalcular también rehace lo existente.
    public ResumenGeneracionResponse generarTodas(boolean recalcular) {
        List<ZonaInfo> zonas = catalogo.zonasActivasConCoordenadas();
        List<SalidaInfo> salidas = catalogo.salidasDisponibles();
        if (zonas.isEmpty() || salidas.isEmpty()) {
            return ResumenGeneracionResponse.vacio();
        }

        Set<Par> existentes = new HashSet<>();
        if (!recalcular) {
            rutaRepository.findBySalidaOrigenIdIn(salidas.stream().map(SalidaInfo::id).toList())
                    .stream()
                    .map(GeneradorRutasExterioresService::parDe)
                    .forEach(existentes::add);
        }

        List<Par> pendientes = salidas.stream()
                .flatMap(s -> masCercanas(s, zonas).stream().map(zona -> new Par(s.id(), zona)))
                .filter(par -> !existentes.contains(par))
                .toList();
        return calcular(pendientes);
    }

    private ResumenGeneracionResponse calcular(List<Par> pares) {
        int generadas = 0;
        int omitidas = 0;
        for (int i = 0; i < pares.size(); i++) {
            Par par = pares.get(i);
            try {
                rutaExteriorService.calcular(par.salidaId(), par.zonaId());
                generadas++;
            } catch (NavegacionNoDisponibleException e) {
                // Si el proveedor no responde, seguir solo acumularía tiempos de espera.
                log.warn("Se interrumpe la generación de rutas: el proveedor de navegación no responde ({})",
                        e.getMessage());
                return new ResumenGeneracionResponse(generadas, omitidas + (pares.size() - i), true);
            } catch (RutaNoEncontradaException | ReglaNegocioException | RecursoNoEncontradoException e) {
                log.warn("No se generó la ruta salida {} -> zona {}: {}", par.salidaId(), par.zonaId(), e.getMessage());
                omitidas++;
            }
        }
        log.info("Generación de rutas exteriores: {} generadas, {} omitidas", generadas, omitidas);
        return new ResumenGeneracionResponse(generadas, omitidas, false);
    }

    // Las N zonas más cercanas en línea recta; evita calcular salidas x zonas llamadas al proveedor.
    private List<UUID> masCercanas(SalidaInfo salida, List<ZonaInfo> zonas) {
        Coordenada origen = new Coordenada(salida.latitud(), salida.longitud());
        return zonas.stream()
                .sorted(Comparator.comparingDouble(
                        z -> origen.distanciaMetrosA(new Coordenada(z.latitud(), z.longitud()))))
                .limit(Math.max(1, propiedades.maxZonasPorSalida()))
                .map(ZonaInfo::id)
                .toList();
    }

    private static Par parDe(RutaEvacuacion ruta) {
        return new Par(ruta.getSalidaOrigenId(), ruta.getZonaSeguraDestinoId());
    }
}
