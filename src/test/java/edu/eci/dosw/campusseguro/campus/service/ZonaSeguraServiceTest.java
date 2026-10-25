package edu.eci.dosw.campusseguro.campus.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.ZonaCodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
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

    @InjectMocks
    private ZonaSeguraService zonaSeguraService;

    private ZonaSeguraRequest request(String codigo) {
        return new ZonaSeguraRequest(
                codigo, "Punto H", "Frente al bloque H", "Zona abierta", 200);
    }

    @Test
    void crear_guardaElCodigoSinEspacios() {
        when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(false);
        when(zonaSeguraRepository.save(any(ZonaSegura.class))).thenAnswer(inv -> {
            ZonaSegura z = inv.getArgument(0);
            z.setId(uuid(1));
            return z;
        });

        ZonaSeguraResponse respuesta =
                zonaSeguraService.crear(request(" MP-H-01 "));

        assertThat(respuesta.codigo()).isEqualTo("MP-H-01");
    }

    @Test
    void crear_conCodigoDuplicado_lanzaExcepcionYNoGuarda() {
        when(zonaSeguraRepository.existsByCodigo("MP-H-01")).thenReturn(true);

        assertThatThrownBy(() ->
                zonaSeguraService.crear(request("MP-H-01")))
                .isInstanceOf(ZonaCodigoDuplicadoException.class);

        verify(zonaSeguraRepository, never()).save(any());
    }

    @Test
    void modificar_conCodigoDeOtraZona_lanzaExcepcion() {
        UUID id = uuid(2);

        when(zonaSeguraRepository.findById(id))
                .thenReturn(Optional.of(new ZonaSegura()));
        when(zonaSeguraRepository.existsByCodigoAndIdNot("MP-H-01", id))
                .thenReturn(true);

        assertThatThrownBy(() ->
                zonaSeguraService.modificar(id, request("MP-H-01")))
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
}