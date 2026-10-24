package edu.eci.dosw.campusseguro.campus.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspacioServiceTest {

    private static final UUID ESPACIO = uuid(10);

    @Mock
    private EspacioRepository espacioRepository;
    @Mock
    private PisoRepository pisoRepository;
    @Mock
    private EdificioRepository edificioRepository;
    @Mock
    private VerificadorRutas verificadorRutas;

    @InjectMocks
    private EspacioService espacioService;

    private Espacio espacioActivo() {
        Espacio e = new Espacio();
        e.setId(ESPACIO);
        e.setEdificioId(uuid(1));
        e.setPisoId(uuid(2));
        e.setCodigo("G-101");
        e.setNombre("Salón 101");
        e.setTipo(TipoEspacio.SALON);
        return e;
    }

    @Test
    void inactivar_sinRutasActivas_loDejaInactivo() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));
        when(verificadorRutas.rutasActivasDeEspacio(ESPACIO)).thenReturn(List.of());
        when(espacioRepository.save(any(Espacio.class))).thenAnswer(inv -> inv.getArgument(0));

        EspacioResponse respuesta = espacioService.inactivar(ESPACIO);

        assertThat(respuesta.estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void inactivar_conRutaActiva_seRechazaYNoGuarda() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));
        when(verificadorRutas.rutasActivasDeEspacio(ESPACIO)).thenReturn(List.of(uuid(20)));

        assertThatThrownBy(() -> espacioService.inactivar(ESPACIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining(uuid(20).toString());
        verify(espacioRepository, never()).save(any());
    }
}
