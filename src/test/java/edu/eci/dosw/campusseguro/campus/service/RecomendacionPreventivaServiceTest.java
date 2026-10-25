package edu.eci.dosw.campusseguro.campus.service;

import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaRequest;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;
import edu.eci.dosw.campusseguro.campus.entity.AmbitoRecomendacion;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.RecomendacionPreventiva;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.RecomendacionPreventivaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecomendacionPreventivaServiceTest {

    private RecomendacionPreventivaRepository repository;
    private EdificioRepository edificioRepository;
    private RecomendacionPreventivaService service;

    private final UUID edificioId = UUID.randomUUID();
    private final UUID recomendacionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(RecomendacionPreventivaRepository.class);
        edificioRepository = mock(EdificioRepository.class);
        service = new RecomendacionPreventivaService(repository, edificioRepository);
    }

    private RecomendacionPreventivaRequest requestEdificio() {
        return new RecomendacionPreventivaRequest(
                AmbitoRecomendacion.EDIFICIO,
                edificioId,
                null,
                " Evacuación segura ",
                " Mantenga libres las salidas de emergencia. ");
    }

    private RecomendacionPreventiva recomendacionActiva() {
        RecomendacionPreventiva r = new RecomendacionPreventiva();
        r.setId(recomendacionId);
        r.setAmbito(AmbitoRecomendacion.EDIFICIO);
        r.setEdificioId(edificioId);
        r.setTitulo("Evacuación segura");
        r.setRecomendacion("Mantenga libres las salidas de emergencia.");
        r.setEstado(Estado.ACTIVO);
        return r;
    }

    @Test
    void listarActivasDevuelveLasRecomendacionesDelRepositorio() {
        when(repository.findByEstado(Estado.ACTIVO))
                .thenReturn(List.of(recomendacionActiva()));

        List<RecomendacionPreventivaResponse> resultado = service.listarActivas();

        assertEquals(1, resultado.size());
        assertEquals("Evacuación segura", resultado.get(0).titulo());
        verify(repository).findByEstado(Estado.ACTIVO);
    }

    @Test
    void consultarPorEdificioDevuelveLasRecomendacionesActivas() {
        when(edificioRepository.existsById(edificioId)).thenReturn(true);
        when(repository.findByEstadoAndEdificioId(Estado.ACTIVO, edificioId))
                .thenReturn(List.of(recomendacionActiva()));

        var resultado = service.consultarPorEdificio(edificioId);

        assertEquals(1, resultado.size());
        verify(repository).findByEstadoAndEdificioId(Estado.ACTIVO, edificioId);
    }

    @Test
    void consultarPorEdificioFallaSiElEdificioNoExiste() {
        when(edificioRepository.existsById(edificioId)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> service.consultarPorEdificio(edificioId));

        verify(repository, never())
                .findByEstadoAndEdificioId(any(), any());
    }

    @Test
    void consultarPorTipoFiltraPorTipoDeEspacio() {
        when(repository.findByEstadoAndTipoEspacio(Estado.ACTIVO, TipoEspacio.SALON))
                .thenReturn(List.of(recomendacionActiva()));

        var resultado = service.consultarPorTipo(TipoEspacio.SALON);

        assertEquals(1, resultado.size());
        verify(repository).findByEstadoAndTipoEspacio(Estado.ACTIVO, TipoEspacio.SALON);
    }

    @Test
    void crearGuardaLaRecomendacionActivaYRecortaLosTextos() {
        when(edificioRepository.existsById(edificioId)).thenReturn(true);
        when(repository.save(any(RecomendacionPreventiva.class)))
                .thenAnswer(invocation -> {
                    RecomendacionPreventiva r = invocation.getArgument(0);
                    r.setId(recomendacionId);
                    return r;
                });

        var resultado = service.crear(requestEdificio());

        assertEquals(recomendacionId, resultado.id());
        assertEquals("Evacuación segura", resultado.titulo());
        assertEquals("Mantenga libres las salidas de emergencia.",
                resultado.recomendacion());
        assertEquals(Estado.ACTIVO, resultado.estado());
        verify(repository).save(any(RecomendacionPreventiva.class));
    }

    @Test
    void crearRechazaEdificioInexistente() {
        when(edificioRepository.existsById(edificioId)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> service.crear(requestEdificio()));

        verify(repository, never()).save(any());
    }

    @Test
    void crearRechazaAmbitoEdificioSinEdificioId() {
        var request = new RecomendacionPreventivaRequest(
                AmbitoRecomendacion.EDIFICIO, null, null,
                "Título", "Recomendación");

        assertThrows(ResponseStatusException.class, () -> service.crear(request));
        verify(repository, never()).save(any());
    }

    @Test
    void crearRechazaAmbitoEdificioConTipoDeEspacio() {
        var request = new RecomendacionPreventivaRequest(
                AmbitoRecomendacion.EDIFICIO, edificioId, TipoEspacio.SALON,
                "Título", "Recomendación");

        assertThrows(ResponseStatusException.class, () -> service.crear(request));
        verify(repository, never()).save(any());
    }

    @Test
    void crearRechazaAmbitoTipoEspacioSinTipo() {
        var request = new RecomendacionPreventivaRequest(
                AmbitoRecomendacion.TIPO_ESPACIO, null, null,
                "Título", "Recomendación");

        assertThrows(ResponseStatusException.class, () -> service.crear(request));
        verify(repository, never()).save(any());
    }

    @Test
    void modificarActualizaUnaRecomendacionExistente() {
        when(edificioRepository.existsById(edificioId)).thenReturn(true);
        when(repository.findById(recomendacionId))
                .thenReturn(Optional.of(recomendacionActiva()));
        when(repository.save(any(RecomendacionPreventiva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.modificar(recomendacionId, requestEdificio());

        assertEquals("Evacuación segura", resultado.titulo());
        verify(repository).save(any(RecomendacionPreventiva.class));
    }

    @Test
    void modificarFallaSiLaRecomendacionNoExiste() {
        when(edificioRepository.existsById(edificioId)).thenReturn(true);
        when(repository.findById(recomendacionId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.modificar(recomendacionId, requestEdificio()));

        verify(repository, never()).save(any());
    }

    @Test
    void inactivarCambiaElEstadoSinEliminarElRegistro() {
        RecomendacionPreventiva existente = recomendacionActiva();
        when(repository.findById(recomendacionId)).thenReturn(Optional.of(existente));
        when(repository.save(any(RecomendacionPreventiva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.inactivar(recomendacionId);

        assertEquals(Estado.INACTIVO, resultado.estado());
        verify(repository).save(existente);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void inactivarFallaSiLaRecomendacionNoExiste() {
        when(repository.findById(recomendacionId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.inactivar(recomendacionId));

        verify(repository, never()).save(any());
    }
}
