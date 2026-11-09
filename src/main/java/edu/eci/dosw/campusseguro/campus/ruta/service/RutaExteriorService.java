package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaExteriorResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.ProveedorNavegacion;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Caso de uso "calcular ruta exterior": salida -> zona segura, guardando el último cálculo (ADR-003, D2).
@Service
public class RutaExteriorService {

    private static final Logger log = LoggerFactory.getLogger(RutaExteriorService.class);

    private final RutaRepository rutaRepository;
    private final SalidaGateway salidaGateway;
    private final ZonaSeguraGateway zonaGateway;
    private final ProveedorNavegacion proveedorNavegacion;

    public RutaExteriorService(RutaRepository rutaRepository,
                               SalidaGateway salidaGateway,
                               ZonaSeguraGateway zonaGateway,
                               ProveedorNavegacion proveedorNavegacion) {
        this.rutaRepository = rutaRepository;
        this.salidaGateway = salidaGateway;
        this.zonaGateway = zonaGateway;
        this.proveedorNavegacion = proveedorNavegacion;
    }

    // Si ya existe la ruta del par salida/zona la recalcula (por ejemplo, si cambiaron las coordenadas).
    // Sin @Transactional a propósito: no se retiene una conexión a la base mientras se espera al proveedor.
    public RutaExteriorResponse calcular(UUID salidaId, UUID zonaSeguraId) {
        SalidaInfo salida = salidaGateway.buscarPorId(salidaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La salida " + salidaId + " no existe"));
        if (!salida.disponible()) {
            throw new ReglaNegocioException("La salida '" + salida.nombre() + "' no está disponible");
        }

        ZonaInfo zona = zonaGateway.buscarPorId(zonaSeguraId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La zona segura " + zonaSeguraId + " no existe"));
        if (!zona.activa()) {
            throw new ReglaNegocioException("La zona segura '" + zona.nombre() + "' está inactiva");
        }
        if (!zona.tieneCoordenadas()) {
            throw new ReglaNegocioException("La zona segura '" + zona.nombre()
                    + "' no tiene coordenadas y no puede ser destino de una ruta exterior");
        }

        RutaCalculada calculada = proveedorNavegacion.calcularRutaPeatonal(
                new Coordenada(salida.latitud(), salida.longitud()),
                new Coordenada(zona.latitud(), zona.longitud()));

        return guardar(salida, zona, calculada);
    }

    private RutaExteriorResponse guardar(SalidaInfo salida, ZonaInfo zona, RutaCalculada calculada) {
        RutaEvacuacion ruta = rutaRepository
                .findBySalidaOrigenIdAndZonaSeguraDestinoId(salida.id(), zona.id())
                .orElseGet(() -> RutaEvacuacion.exterior(salida.id(), zona.id(), null));
        ruta.setDescripcion("Ruta exterior desde la salida '" + salida.nombre()
                + "' hasta la zona segura '" + zona.nombre() + "'");
        ruta.registrarCalculo(
                calculada.distanciaMetros(),
                calculada.duracionSegundos(),
                CodificadorRutaExterior.codificarGeometria(calculada.geometria()),
                CodificadorRutaExterior.codificarPasos(calculada.pasos()),
                Instant.now());
        RutaEvacuacion guardada = rutaRepository.save(ruta);
        log.info("Ruta exterior {} calculada: salida {} -> zona {} ({} m)",
                guardada.getId(), salida.id(), zona.id(), guardada.getDistanciaMetros());
        return aResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<RutaExteriorResponse> listarPorSalida(UUID salidaId) {
        if (salidaGateway.buscarPorId(salidaId).isEmpty()) {
            throw new RecursoNoEncontradoException("La salida " + salidaId + " no existe");
        }
        return rutaRepository.findBySalidaOrigenId(salidaId).stream()
                .map(RutaExteriorService::aResponse)
                .toList();
    }

    static RutaExteriorResponse aResponse(RutaEvacuacion ruta) {
        return new RutaExteriorResponse(
                ruta.getId(),
                ruta.getSalidaOrigenId(),
                ruta.getZonaSeguraDestinoId(),
                ruta.getEstado(),
                ruta.getMotivo(),
                ruta.getDistanciaMetros(),
                ruta.getDuracionSegundos(),
                CodificadorRutaExterior.decodificarGeometria(ruta.getGeometria()),
                CodificadorRutaExterior.decodificarPasos(ruta.getPasos()),
                ruta.getCalculadaEn());
    }
}
