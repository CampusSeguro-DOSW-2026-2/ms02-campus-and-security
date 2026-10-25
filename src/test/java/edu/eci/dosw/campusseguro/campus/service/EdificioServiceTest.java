package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eci.dosw.campusseguro.campus.dto.EdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EdificioResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdificioServiceTest {

    @Mock
    private EdificioRepository edificioRepository;

    @InjectMocks
    private EdificioService edificioService;

    private Edificio nuevoEdificio(UUID id, String codigo, String nombre) {
        Edificio e = new Edificio();
        e.setId(id);
        e.setCodigo(codigo);
        e.setNombre(nombre);
        return e;
    }

    @Test
    void crear_guardaEdificioActivoYLimpiaEspacios() {
        when(edificioRepository.existsByCodigo("ED-1")).thenReturn(false);
        when(edificioRepository.save(any(Edificio.class))).thenAnswer(inv -> {
            Edificio e = inv.getArgument(0);
            e.setId(uuid(1));
            return e;
        });

        EdificioResponse respuesta = edificioService.crear(
                new EdificioRequest(" ED-1 ", " Bloque H ", "Descripción"));

        assertThat(respuesta.id()).isEqualTo(uuid(1));
        assertThat(respuesta.codigo()).isEqualTo("ED-1");
        assertThat(respuesta.nombre()).isEqualTo("Bloque H");
        assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void crear_codigoDuplicado_lanzaExcepcionYNoGuarda() {
        when(edificioRepository.existsByCodigo("ED-1")).thenReturn(true);

        assertThatThrownBy(() -> edificioService.crear(
                new EdificioRequest("ED-1", "Bloque H", null)))
                .isInstanceOf(CodigoDuplicadoException.class);

        verify(edificioRepository, never()).save(any());
    }

    @Test
    void modificar_actualizaLosCampos() {
        when(edificioRepository.findById(uuid(1)))
                .thenReturn(Optional.of(nuevoEdificio(uuid(1), "ED-1", "Viejo")));
        when(edificioRepository.save(any(Edificio.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EdificioResponse respuesta = edificioService.modificar(
                uuid(1), new ModificarEdificioRequest("Nuevo", "Otra descripción"));

        assertThat(respuesta.codigo()).isEqualTo("ED-1");
        assertThat(respuesta.nombre()).isEqualTo("Nuevo");
        assertThat(respuesta.descripcion()).isEqualTo("Otra descripción");
    }

    @Test
    void modificar_noCambiaElCodigo() {
        UUID id = uuid(1);
        Edificio existente = nuevoEdificio(id, "ED-1", "Bloque H");
        when(edificioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(edificioRepository.save(any(Edificio.class))).thenAnswer(inv -> inv.getArgument(0));

        EdificioResponse respuesta = edificioService.modificar(id,
                new ModificarEdificioRequest(" Bloque H nuevo ", "Otra descripción"));

        assertThat(respuesta.codigo()).isEqualTo("ED-1");
        assertThat(respuesta.nombre()).isEqualTo("Bloque H nuevo");
    }

    @Test
    void modificar_noExiste_lanzaExcepcion() {
        when(edificioRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> edificioService.modificar(
                uuid(99), new ModificarEdificioRequest("Nuevo", null)))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void inactivar_cambiaElEstadoAInactivo() {
        when(edificioRepository.findById(uuid(1)))
                .thenReturn(Optional.of(nuevoEdificio(uuid(1), "ED-1", "Bloque H")));
        when(edificioRepository.save(any(Edificio.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EdificioResponse respuesta = edificioService.inactivar(uuid(1));

        assertThat(respuesta.estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void inactivar_noExiste_lanzaExcepcion() {
        when(edificioRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> edificioService.inactivar(uuid(99)))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void listar_devuelveTodosLosEdificios() {
        when(edificioRepository.findAll()).thenReturn(List.of(
                nuevoEdificio(uuid(1), "ED-1", "Bloque H"),
                nuevoEdificio(uuid(2), "ED-2", "Bloque B")));

        assertThat(edificioService.listar()).hasSize(2);
    }

    @Test
    void obtener_existente_devuelveElEdificio() {
        when(edificioRepository.findById(uuid(1)))
                .thenReturn(Optional.of(nuevoEdificio(uuid(1), "ED-1", "Bloque H")));

        assertThat(edificioService.obtener(uuid(1)).codigo()).isEqualTo("ED-1");
    }

    @Test
    void obtener_noExiste_lanzaExcepcion() {
        when(edificioRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> edificioService.obtener(uuid(99)))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }
}