package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioPublicoResponse;
import edu.eci.dosw.campusseguro.campus.dto.SalidaPublicaResponse;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraPublicaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.ImagenRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.ConsultaRutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaPublicaService {

    static final String AVISO_NO_DISPONIBLE =
            "Este lugar no está disponible en este momento. Siga las indicaciones del personal de emergencia.";

    private final EspacioRepository espacioRepository;
    private final SalidaRepository salidaRepository;
    private final ZonaSeguraRepository zonaSeguraRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;
    private final ImagenRepository imagenRepository;
    private final ConsultaRutaService consultaRutaService;

    @Transactional(readOnly = true)
    public EspacioPublicoResponse espacio(UUID id) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException(id));
        Piso piso = pisoRepository.findById(espacio.getPisoId()).orElse(null);
        Edificio edificio = edificioRepository.findById(espacio.getEdificioId()).orElse(null);
        boolean disponible = activo(espacio.getEstado()) && activo(piso) && activo(edificio);
        boolean tieneFotografia = imagenRepository.existsByTipoElementoAndElementoId(
                TipoElementoImagen.ESPACIO, id);

        return new EspacioPublicoResponse(
                espacio.getCodigo(),
                espacio.getNombre(),
                espacio.getTipo(),
                edificio == null ? null : edificio.getNombre(),
                piso == null ? null : piso.getNumero(),
                disponible,
                disponible ? evacuacion(consultaRutaService.consultarPorEspacio(id)) : null,
                disponible ? null : AVISO_NO_DISPONIBLE,
                tieneFotografia);
    }

    @Transactional(readOnly = true)
    public SalidaPublicaResponse salida(UUID id) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new SalidaNoEncontradaException(id));
        Piso piso = pisoRepository.findById(salida.getPisoId()).orElse(null);
        Edificio edificio = edificioRepository.findById(salida.getEdificioId()).orElse(null);
        boolean disponible = activo(salida.getEstado()) && activo(piso) && activo(edificio);
        boolean tieneFotografia = imagenRepository.existsByTipoElementoAndElementoId(
                TipoElementoImagen.SALIDA, id);

        return new SalidaPublicaResponse(
                salida.getCodigo(),
                salida.getNombre(),
                edificio == null ? null : edificio.getNombre(),
                piso == null ? null : piso.getNumero(),
                salida.getLatitud(),
                salida.getLongitud(),
                disponible,
                disponible ? null : AVISO_NO_DISPONIBLE,
                tieneFotografia);
    }

    @Transactional(readOnly = true)
    public ZonaSeguraPublicaResponse zonaSegura(UUID id) {
        ZonaSegura zona = zonaSeguraRepository.findById(id)
                .orElseThrow(() -> new ZonaSeguraNoEncontradaException(id));
        boolean disponible = activo(zona.getEstado());
        boolean tieneFotografia = imagenRepository.existsByTipoElementoAndElementoId(
                TipoElementoImagen.ZONA_SEGURA, id);

        return new ZonaSeguraPublicaResponse(
                zona.getCodigo(),
                zona.getNombre(),
                zona.getUbicacion(),
                zona.isPuntoEncuentro(),
                zona.getLatitud(),
                zona.getLongitud(),
                disponible,
                disponible ? null : AVISO_NO_DISPONIBLE,
                tieneFotografia);
    }

    private static EspacioPublicoResponse.Evacuacion evacuacion(RutaConsultadaResponse ruta) {
        List<EspacioPublicoResponse.Recomendacion> recomendaciones = ruta.recomendacionesPreventivas() == null
                ? List.of()
                : ruta.recomendacionesPreventivas().stream()
                        .map(r -> new EspacioPublicoResponse.Recomendacion(r.titulo(), r.recomendacion()))
                        .toList();

        return new EspacioPublicoResponse.Evacuacion(
                ruta.tieneRuta(),
                ruta.zonaSeguraNombre(),
                ruta.zonaSeguraUbicacion(),
                ruta.recorrido(),
                ruta.estado(),
                ruta.motivo(),
                ruta.mensaje(),
                recomendaciones);
    }

    private static boolean activo(Estado estado) {
        return estado == Estado.ACTIVO;
    }

    private static boolean activo(Piso piso) {
        return piso != null && activo(piso.getEstado());
    }

    private static boolean activo(Edificio edificio) {
        return edificio != null && activo(edificio.getEstado());
    }
}
