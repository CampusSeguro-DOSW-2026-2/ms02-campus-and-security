package co.edu.escuelaing.campusseguro.campus.service;

import co.edu.escuelaing.campusseguro.campus.dto.EdificioRequest;
import co.edu.escuelaing.campusseguro.campus.dto.EdificioResponse;
import co.edu.escuelaing.campusseguro.campus.entity.Edificio;
import co.edu.escuelaing.campusseguro.campus.entity.Estado;
import co.edu.escuelaing.campusseguro.campus.exception.CodigoDuplicadoException;
import co.edu.escuelaing.campusseguro.campus.exception.EdificioNoEncontradoException;
import co.edu.escuelaing.campusseguro.campus.repository.EdificioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdificioServiceTest {

    @Mock
    private EdificioRepository edificioRepository;

    @InjectMocks
    private EdificioService edificioService;

    private Edificio nuevoEdificio(Long id, String codigo, String nombre) {
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
            e.setId(1L);
            return e;
        });

        EdificioResponse respuesta = edificioService.crear(
                new EdificioRequest(" ED-1 ", " Bloque H ", "Descripción"));

        assertThat(respuesta.id()).isEqualTo(1L);
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
        when(edificioRepository.findById(1L))
                .thenReturn(Optional.of(nuevoEdificio(1L, "ED-1", "Viejo")));
        when(edificioRepository.existsByCodigoAndIdNot("ED-2", 1L)).thenReturn(false);
        when(edificioRepository.save(any(Edificio.class))).thenAnswer(inv -> inv.getArgument(0));

        EdificioResponse respuesta = edificioService.modificar(
                1L, new EdificioRequest("ED-2", "Nuevo", "Otra descripción"));

        assertThat(respuesta.codigo()).isEqualTo("ED-2");
        assertThat(respuesta.nombre()).isEqualTo("Nuevo");
        assertThat(respuesta.descripcion()).isEqualTo("Otra descripción");
    }

    @Test
    void modificar_codigoDeOtroEdificio_lanzaExcepcion() {
        when(edificioRepository.findById(1L))
                .thenReturn(Optional.of(nuevoEdificio(1L, "ED-1", "Viejo")));
        when(edificioRepository.existsByCodigoAndIdNot("ED-2", 1L)).thenReturn(true);

        assertThatThrownBy(() -> edificioService.modificar(
                1L, new EdificioRequest("ED-2", "Nuevo", null)))
                .isInstanceOf(CodigoDuplicadoException.class);

        verify(edificioRepository, never()).save(any());
    }

    @Test
    void modificar_noExiste_lanzaExcepcion() {
        when(edificioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> edificioService.modificar(
                99L, new EdificioRequest("ED-2", "Nuevo", null)))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void inactivar_cambiaElEstadoAInactivo() {
        when(edificioRepository.findById(1L))
                .thenReturn(Optional.of(nuevoEdificio(1L, "ED-1", "Bloque H")));
        when(edificioRepository.save(any(Edificio.class))).thenAnswer(inv -> inv.getArgument(0));

        EdificioResponse respuesta = edificioService.inactivar(1L);

        assertThat(respuesta.estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void inactivar_noExiste_lanzaExcepcion() {
        when(edificioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> edificioService.inactivar(99L))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void listar_devuelveTodosLosEdificios() {
        when(edificioRepository.findAll()).thenReturn(List.of(
                nuevoEdificio(1L, "ED-1", "Bloque H"),
                nuevoEdificio(2L, "ED-2", "Bloque B")));

        assertThat(edificioService.listar()).hasSize(2);
    }

    @Test
    void obtener_existente_devuelveElEdificio() {
        when(edificioRepository.findById(1L))
                .thenReturn(Optional.of(nuevoEdificio(1L, "ED-1", "Bloque H")));

        assertThat(edificioService.obtener(1L).codigo()).isEqualTo("ED-1");
    }

    @Test
    void obtener_noExiste_lanzaExcepcion() {
        when(edificioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> edificioService.obtener(99L))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }
}