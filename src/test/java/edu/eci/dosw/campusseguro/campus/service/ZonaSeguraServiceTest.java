package edu.eci.dosw.campusseguro.campus.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ModificarZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaAbierta;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.ZonaAbiertaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaCodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.ZonaAbiertaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZonaSeguraServiceTest {

        @Mock
        private ZonaSeguraRepository zonaSeguraRepository;

        @Mock
        private VerificadorRutas verificadorRutas;

        @Mock
        private ZonaAbiertaRepository zonaAbiertaRepository;

        @InjectMocks
        private ZonaSeguraService zonaSeguraService;

        private ZonaSeguraRequest request(String codigo) {
                return new ZonaSeguraRequest(
                                codigo, "Punto H", "Frente al bloque H", "Zona abierta", 200, null, null, null, null);
        }

        @Test
        void crear_guardaElCodigoSinEspacios() {
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> {
                        ZonaSegura z = inv.getArgument(0);
                        z.setId(uuid(1));
                        return z;
                });

                ZonaSeguraResponse respuesta = zonaSeguraService.crear(request(" MP-H-01 "));

                assertThat(respuesta.codigo()).isEqualTo("MP-H-01");
        }

        @Test
        void crear_conCodigoDuplicado_lanzaExcepcionYNoGuarda() {
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(true);

                assertThatThrownBy(() -> zonaSeguraService.crear(request("MP-H-01")))
                                .isInstanceOf(ZonaCodigoDuplicadoException.class);

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void inactivar_sinRutasActivas_cambiaElEstadoAInactivo() {
                ZonaSegura zona = new ZonaSegura();
                zona.setId(uuid(1));

                when(zonaSeguraRepository.findById(uuid(1)))
                                .thenReturn(Optional.of(zona));
                when(verificadorRutas.rutasActivasDeZonaSegura(uuid(1)))
                                .thenReturn(List.of());
                when(zonaSeguraRepository.save(any(ZonaSegura.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.inactivar(uuid(1));

                assertThat(respuesta.estado()).isEqualTo(Estado.INACTIVO);
                verify(verificadorRutas).rutasActivasDeZonaSegura(uuid(1));
        }

        @Test
        void inactivar_conRutasActivas_lanzaReglaDeNegocioYNoGuarda() {
                UUID ruta = uuid(50);
                ZonaSegura zona = new ZonaSegura();
                zona.setId(uuid(1));

                when(zonaSeguraRepository.findById(uuid(1)))
                                .thenReturn(Optional.of(zona));
                when(verificadorRutas.rutasActivasDeZonaSegura(uuid(1)))
                                .thenReturn(List.of(ruta));

                assertThatThrownBy(() -> zonaSeguraService.inactivar(uuid(1)))
                                .isInstanceOf(ReglaNegocioException.class)
                                .hasMessageContaining(ruta.toString());

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void inactivar_noExiste_lanzaExcepcion() {
                when(zonaSeguraRepository.findById(uuid(99)))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> zonaSeguraService.inactivar(uuid(99)))
                                .isInstanceOf(ZonaSeguraNoEncontradaException.class);
        }

        @Test
        void listar_porDefectoDevuelveSoloLasActivas() {
                ZonaSegura activa = new ZonaSegura();
                activa.setId(uuid(1));
                activa.setCodigo("MP-H-01");

                ZonaSegura inactiva = new ZonaSegura();
                inactiva.setId(uuid(2));
                inactiva.setCodigo("MP-H-02");
                inactiva.setEstado(Estado.INACTIVO);

                when(zonaSeguraRepository.findAll()).thenReturn(List.of(activa, inactiva));

                assertThat(zonaSeguraService.listar(false))
                                .extracting(ZonaSeguraResponse::codigo)
                                .containsExactly("MP-H-01");
        }

        @Test
        void listar_conIncluirInactivos_devuelveTodas() {
                ZonaSegura activa = new ZonaSegura();
                activa.setId(uuid(1));

                ZonaSegura inactiva = new ZonaSegura();
                inactiva.setId(uuid(2));
                inactiva.setEstado(Estado.INACTIVO);

                when(zonaSeguraRepository.findAll()).thenReturn(List.of(activa, inactiva));

                assertThat(zonaSeguraService.listar(true)).hasSize(2);
        }

        // Pruebas nuevas de reactivación

        @Test
        void reactivar_zonaInactiva_cambiaElEstadoAActivo() {
                ZonaSegura zona = new ZonaSegura();
                zona.setId(uuid(1));
                zona.setEstado(Estado.INACTIVO);

                when(zonaSeguraRepository.findById(uuid(1)))
                                .thenReturn(Optional.of(zona));
                when(zonaSeguraRepository.save(any(ZonaSegura.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.reactivar(uuid(1));

                assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
                verify(zonaSeguraRepository).save(zona);
        }

        @Test
        void reactivar_zonaYaActiva_lanzaExcepcionYNoGuarda() {
                ZonaSegura zona = new ZonaSegura();
                zona.setId(uuid(1));
                zona.setEstado(Estado.ACTIVO);

                when(zonaSeguraRepository.findById(uuid(1)))
                                .thenReturn(Optional.of(zona));

                assertThatThrownBy(() -> zonaSeguraService.reactivar(uuid(1)))
                                .isInstanceOf(ReglaNegocioException.class)
                                .hasMessageContaining("La zona segura ya está activa");

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void reactivar_noExiste_lanzaExcepcion() {
                when(zonaSeguraRepository.findById(uuid(99)))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> zonaSeguraService.reactivar(uuid(99)))
                                .isInstanceOf(ZonaSeguraNoEncontradaException.class);
        }

        @Test
        void modificar_noCambiaElCodigo() {
                ZonaSegura zona = new ZonaSegura();
                zona.setId(uuid(1));
                zona.setCodigo("MP-H-01");
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zona));
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.modificar(uuid(1),
                                new ModificarZonaSeguraRequest(" Punto H nuevo ", "Otra ubicación", "Otra descripción",
                                                300, null, null, null, null));

                assertThat(respuesta.codigo()).isEqualTo("MP-H-01");
                assertThat(respuesta.nombre()).isEqualTo("Punto H nuevo");
                assertThat(respuesta.capacidad()).isEqualTo(300);
        }

        // ---------- coordenadas, zona abierta y punto de encuentro ----------

        private static final BigDecimal LAT = new BigDecimal("4.782000");
        private static final BigDecimal LNG = new BigDecimal("-74.042000");

        private ZonaSeguraRequest requestConUbicacion(BigDecimal lat, BigDecimal lng, UUID zonaAbiertaId,
                        Boolean puntoEncuentro) {
                return new ZonaSeguraRequest("MP-H-01", "Punto H", "Frente al bloque H", "Zona abierta", 200,
                                lat, lng, zonaAbiertaId, puntoEncuentro);
        }

        private ModificarZonaSeguraRequest modificarCon(BigDecimal lat, BigDecimal lng, UUID zonaAbiertaId,
                        Boolean puntoEncuentro) {
                return new ModificarZonaSeguraRequest("Punto H", "Frente al bloque H", "Zona abierta", 200,
                                lat, lng, zonaAbiertaId, puntoEncuentro);
        }

        private void zonaAbierta(UUID id, Estado estado) {
                ZonaAbierta a = new ZonaAbierta();
                a.setId(id);
                a.setEstado(estado);
                when(zonaAbiertaRepository.findById(id)).thenReturn(Optional.of(a));
        }

        private ZonaSegura zonaConUbicacion() {
                ZonaSegura zona = new ZonaSegura();
                zona.setId(uuid(1));
                zona.setCodigo("MP-H-01");
                zona.setLatitud(LAT);
                zona.setLongitud(LNG);
                zona.setZonaAbiertaId(uuid(10));
                return zona;
        }

        @Test
        void crear_conCoordenadasYZonaAbierta_guardaLosDatos() {
                zonaAbierta(uuid(10), Estado.ACTIVO);
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.crear(requestConUbicacion(LAT, LNG, uuid(10), false));

                assertThat(respuesta.latitud()).isEqualTo(LAT);
                assertThat(respuesta.longitud()).isEqualTo(LNG);
                assertThat(respuesta.zonaAbiertaId()).isEqualTo(uuid(10));
                assertThat(respuesta.puntoEncuentro()).isFalse();
        }

        @Test
        void crear_sinIndicarPuntoEncuentro_quedaComoPuntoDeEncuentro() {
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.crear(request("MP-H-01"));

                assertThat(respuesta.puntoEncuentro()).isTrue();
                assertThat(respuesta.latitud()).isNull();
                assertThat(respuesta.zonaAbiertaId()).isNull();
        }

        @Test
        void crear_soloConLatitud_lanzaReglaDeNegocioYNoGuarda() {
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);

                assertThatThrownBy(() -> zonaSeguraService.crear(requestConUbicacion(LAT, null, null, null)))
                                .isInstanceOf(ReglaNegocioException.class)
                                .hasMessageContaining("juntas");

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void crear_zonaAbiertaInexistente_lanzaExcepcionYNoGuarda() {
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);
                when(zonaAbiertaRepository.findById(uuid(99))).thenReturn(Optional.empty());

                assertThatThrownBy(() -> zonaSeguraService.crear(requestConUbicacion(null, null, uuid(99), null)))
                                .isInstanceOf(ZonaAbiertaNoEncontradaException.class);

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void crear_zonaAbiertaInactiva_lanzaReglaDeNegocioYNoGuarda() {
                when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);
                zonaAbierta(uuid(10), Estado.INACTIVO);

                assertThatThrownBy(() -> zonaSeguraService.crear(requestConUbicacion(null, null, uuid(10), null)))
                                .isInstanceOf(ReglaNegocioException.class);

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void modificar_sinEnviarUbicacion_conservaLosDatosActuales() {
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zonaConUbicacion()));
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.modificar(uuid(1), modificarCon(null, null, null, null));

                assertThat(respuesta.latitud()).isEqualTo(LAT);
                assertThat(respuesta.longitud()).isEqualTo(LNG);
                assertThat(respuesta.zonaAbiertaId()).isEqualTo(uuid(10));
                assertThat(respuesta.puntoEncuentro()).isTrue();
        }

        @Test
        void modificar_conCoordenadasNuevas_lasActualiza() {
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zonaConUbicacion()));
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.modificar(uuid(1),
                                modificarCon(new BigDecimal("4.790000"), new BigDecimal("-74.050000"), null, null));

                assertThat(respuesta.latitud()).isEqualTo(new BigDecimal("4.790000"));
                assertThat(respuesta.longitud()).isEqualTo(new BigDecimal("-74.050000"));
        }

        @Test
        void modificar_soloConLongitud_lanzaReglaDeNegocioYNoGuarda() {
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zonaConUbicacion()));

                assertThatThrownBy(() -> zonaSeguraService.modificar(uuid(1), modificarCon(null, LNG, null, null)))
                                .isInstanceOf(ReglaNegocioException.class);

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void modificar_conZonaAbiertaNueva_laCambia() {
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zonaConUbicacion()));
                zonaAbierta(uuid(11), Estado.ACTIVO);
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.modificar(uuid(1), modificarCon(null, null, uuid(11), null));

                assertThat(respuesta.zonaAbiertaId()).isEqualTo(uuid(11));
        }

        @Test
        void modificar_cambiaLaMarcaDePuntoEncuentro() {
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zonaConUbicacion()));
                when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> inv.getArgument(0));

                ZonaSeguraResponse respuesta = zonaSeguraService.modificar(uuid(1), modificarCon(null, null, null, false));

                assertThat(respuesta.puntoEncuentro()).isFalse();
        }

        @Test
        void reactivar_conZonaAbiertaInactiva_lanzaReglaDeNegocioYNoGuarda() {
                ZonaSegura zona = zonaConUbicacion();
                zona.setEstado(Estado.INACTIVO);
                when(zonaSeguraRepository.findById(uuid(1))).thenReturn(Optional.of(zona));
                zonaAbierta(uuid(10), Estado.INACTIVO);

                assertThatThrownBy(() -> zonaSeguraService.reactivar(uuid(1)))
                                .isInstanceOf(ReglaNegocioException.class)
                                .hasMessageContaining("zona abierta");

                verify(zonaSeguraRepository, never()).save(any());
        }

        @Test
        void listar_conPuntoEncuentro_filtraPorLaMarca() {
                ZonaSegura punto = new ZonaSegura();
                punto.setId(uuid(1));
                punto.setCodigo("MP-H-01");
                ZonaSegura noPunto = new ZonaSegura();
                noPunto.setId(uuid(2));
                noPunto.setCodigo("ZS-02");
                noPunto.setPuntoEncuentro(false);
                when(zonaSeguraRepository.findAll()).thenReturn(List.of(punto, noPunto));

                assertThat(zonaSeguraService.listar(false, true, false))
                                .extracting(ZonaSeguraResponse::codigo).containsExactly("MP-H-01");
                assertThat(zonaSeguraService.listar(false, false, false))
                                .extracting(ZonaSeguraResponse::codigo).containsExactly("ZS-02");
        }

        @Test
        void listar_sinCoordenadas_devuelveSoloLasQueNoLasTienen() {
                ZonaSegura sinCoordenadas = new ZonaSegura();
                sinCoordenadas.setId(uuid(2));
                sinCoordenadas.setCodigo("ZS-02");
                when(zonaSeguraRepository.findAll()).thenReturn(List.of(zonaConUbicacion(), sinCoordenadas));

                assertThat(zonaSeguraService.listar(false, null, true))
                                .extracting(ZonaSeguraResponse::codigo).containsExactly("ZS-02");
        }
}
