package edu.eci.dosw.campusseguro.campus.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioPublicoResponse;
import edu.eci.dosw.campusseguro.campus.dto.SalidaPublicaResponse;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraPublicaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.ConsultaRutaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaPublicaServiceTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID ESPACIO = uuid(10);
    private static final UUID SALIDA = uuid(20);
    private static final UUID ZONA = uuid(30);

    @Mock
    private EspacioRepository espacioRepository;
    @Mock
    private SalidaRepository salidaRepository;
    @Mock
    private ZonaSeguraRepository zonaSeguraRepository;
    @Mock
    private PisoRepository pisoRepository;
    @Mock
    private EdificioRepository edificioRepository;
    @Mock
    private ConsultaRutaService consultaRutaService;

    @InjectMocks
    private ConsultaPublicaService servicio;

    private void contenedores(Estado edificio, Estado piso) {
        Edificio e = new Edificio();
        e.setId(EDIFICIO);
        e.setNombre("Bloque G");
        e.setEstado(edificio);
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(e));
        Piso p = new Piso();
        p.setId(PISO);
        p.setEdificioId(EDIFICIO);
        p.setNumero(2);
        p.setEstado(piso);
        when(pisoRepository.findById(PISO)).thenReturn(Optional.of(p));
    }

    private void espacio(Estado estado) {
        Espacio e = new Espacio();
        e.setId(ESPACIO);
        e.setEdificioId(EDIFICIO);
        e.setPisoId(PISO);
        e.setCodigo("G-201");
        e.setNombre("Salón 201");
        e.setTipo(TipoEspacio.SALON);
        e.setEstado(estado);
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(e));
    }

    private static RutaConsultadaResponse ruta() {
        RecomendacionPreventivaResponse recomendacion = new RecomendacionPreventivaResponse(uuid(40), null, EDIFICIO,
                null, "Escaleras", "Use la escalera norte.", Estado.ACTIVO, null, null);
        return new RutaConsultadaResponse(ESPACIO, "Salón 201", true, ZONA, "Plazoleta H", "Frente al bloque H",
                "Baje por la escalera norte.", EstadoRuta.RESTRINGIDA, "Obra en la salida lateral", true,
                "La ruta está restringida.", List.of(recomendacion));
    }

    @Test
    void espacio_disponible_devuelveEdificioPisoYEvacuacion() {
        espacio(Estado.ACTIVO);
        contenedores(Estado.ACTIVO, Estado.ACTIVO);
        when(consultaRutaService.consultarPorEspacio(ESPACIO)).thenReturn(ruta());

        EspacioPublicoResponse r = servicio.espacio(ESPACIO);

        assertThat(r.nombre()).isEqualTo("Salón 201");
        assertThat(r.codigo()).isEqualTo("G-201");
        assertThat(r.edificio()).isEqualTo("Bloque G");
        assertThat(r.piso()).isEqualTo(2);
        assertThat(r.disponible()).isTrue();
        assertThat(r.aviso()).isNull();
        assertThat(r.evacuacion().zonaSegura()).isEqualTo("Plazoleta H");
        assertThat(r.evacuacion().recorrido()).isEqualTo("Baje por la escalera norte.");
        assertThat(r.evacuacion().estado()).isEqualTo(EstadoRuta.RESTRINGIDA);
        assertThat(r.evacuacion().mensaje()).isEqualTo("La ruta está restringida.");
        assertThat(r.evacuacion().recomendaciones()).extracting(EspacioPublicoResponse.Recomendacion::titulo)
                .containsExactly("Escaleras");
    }

    @Test
    void espacio_inactivo_devuelveAvisoSinConsultarLaRuta() {
        espacio(Estado.INACTIVO);
        contenedores(Estado.ACTIVO, Estado.ACTIVO);

        EspacioPublicoResponse r = servicio.espacio(ESPACIO);

        assertThat(r.disponible()).isFalse();
        assertThat(r.evacuacion()).isNull();
        assertThat(r.aviso()).isEqualTo(ConsultaPublicaService.AVISO_NO_DISPONIBLE);
        verify(consultaRutaService, never()).consultarPorEspacio(any());
    }

    @Test
    void espacio_enPisoOEdificioInactivo_noEstaDisponible() {
        espacio(Estado.ACTIVO);
        contenedores(Estado.ACTIVO, Estado.INACTIVO);

        assertThat(servicio.espacio(ESPACIO).disponible()).isFalse();
        verify(consultaRutaService, never()).consultarPorEspacio(any());
    }

    @Test
    void espacio_inexistente_falla() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.espacio(ESPACIO)).isInstanceOf(EspacioNoEncontradoException.class);
    }

    @Test
    void salida_devuelveSusCoordenadas() {
        Salida s = new Salida();
        s.setId(SALIDA);
        s.setEdificioId(EDIFICIO);
        s.setPisoId(PISO);
        s.setCodigo("G-S1");
        s.setNombre("Salida principal");
        s.setLatitud(new BigDecimal("4.782710"));
        s.setLongitud(new BigDecimal("-74.042630"));
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(s));
        contenedores(Estado.ACTIVO, Estado.ACTIVO);

        SalidaPublicaResponse r = servicio.salida(SALIDA);

        assertThat(r.nombre()).isEqualTo("Salida principal");
        assertThat(r.edificio()).isEqualTo("Bloque G");
        assertThat(r.piso()).isEqualTo(2);
        assertThat(r.latitud()).isEqualByComparingTo("4.78271");
        assertThat(r.disponible()).isTrue();
        assertThat(r.aviso()).isNull();
    }

    @Test
    void salida_inexistente_falla() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.salida(SALIDA)).isInstanceOf(SalidaNoEncontradaException.class);
    }

    @Test
    void zonaSegura_inactiva_avisaQueNoEstaDisponible() {
        ZonaSegura z = new ZonaSegura();
        z.setId(ZONA);
        z.setCodigo("MP-H-01");
        z.setNombre("Plazoleta H");
        z.setUbicacion("Frente al bloque H");
        z.setLatitud(new BigDecimal("4.783100"));
        z.setLongitud(new BigDecimal("-74.043100"));
        z.setEstado(Estado.INACTIVO);
        when(zonaSeguraRepository.findById(ZONA)).thenReturn(Optional.of(z));

        ZonaSeguraPublicaResponse r = servicio.zonaSegura(ZONA);

        assertThat(r.codigo()).isEqualTo("MP-H-01");
        assertThat(r.puntoEncuentro()).isTrue();
        assertThat(r.disponible()).isFalse();
        assertThat(r.aviso()).isEqualTo(ConsultaPublicaService.AVISO_NO_DISPONIBLE);
    }

    @Test
    void zonaSegura_inexistente_falla() {
        when(zonaSeguraRepository.findById(ZONA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.zonaSegura(ZONA)).isInstanceOf(ZonaSeguraNoEncontradaException.class);
    }

    @Test
    void espacio_sinPisoNiEdificioRegistrados_noEstaDisponibleYNoFalla() {
        espacio(Estado.ACTIVO);
        when(pisoRepository.findById(PISO)).thenReturn(Optional.empty());
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        EspacioPublicoResponse r = servicio.espacio(ESPACIO);

        assertThat(r.disponible()).isFalse();
        assertThat(r.edificio()).isNull();
        assertThat(r.piso()).isNull();
    }

    @Test
    void espacio_sinRecomendaciones_devuelveListaVacia() {
        espacio(Estado.ACTIVO);
        contenedores(Estado.ACTIVO, Estado.ACTIVO);
        when(consultaRutaService.consultarPorEspacio(ESPACIO)).thenReturn(new RutaConsultadaResponse(ESPACIO,
                "Salón 201", false, null, null, null, null, null, null, false,
                "No existe una ruta recomendada para la selección.", null));

        EspacioPublicoResponse.Evacuacion evacuacion = servicio.espacio(ESPACIO).evacuacion();

        assertThat(evacuacion.tieneRuta()).isFalse();
        assertThat(evacuacion.recomendaciones()).isEmpty();
    }

    @Test
    void salida_inactivaOEnEdificioInactivo_avisaQueNoEstaDisponible() {
        Salida s = new Salida();
        s.setId(SALIDA);
        s.setEdificioId(EDIFICIO);
        s.setPisoId(PISO);
        s.setEstado(Estado.ACTIVO);
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(s));
        contenedores(Estado.INACTIVO, Estado.ACTIVO);

        SalidaPublicaResponse r = servicio.salida(SALIDA);

        assertThat(r.disponible()).isFalse();
        assertThat(r.aviso()).isEqualTo(ConsultaPublicaService.AVISO_NO_DISPONIBLE);
    }

    @Test
    void zonaSegura_activaQueNoEsPuntoDeEncuentro() {
        ZonaSegura z = new ZonaSegura();
        z.setId(ZONA);
        z.setNombre("Cancha");
        z.setPuntoEncuentro(false);
        when(zonaSeguraRepository.findById(ZONA)).thenReturn(Optional.of(z));

        ZonaSeguraPublicaResponse r = servicio.zonaSegura(ZONA);

        assertThat(r.disponible()).isTrue();
        assertThat(r.puntoEncuentro()).isFalse();
        assertThat(r.aviso()).isNull();
    }
}
