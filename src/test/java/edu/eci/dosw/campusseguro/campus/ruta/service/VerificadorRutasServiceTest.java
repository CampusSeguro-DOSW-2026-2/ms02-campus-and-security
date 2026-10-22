package edu.eci.dosw.campusseguro.campus.ruta.service;

import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificadorRutasServiceTest {

    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private EspacioGateway espacioGateway;

    private VerificadorRutasService verificador;

    private final Long edificioId = 10L;
    private final Long espacioId = 11L;
    private final Long zonaId = 12L;

    @BeforeEach
    void setUp() {
        verificador = new VerificadorRutasService(rutaRepository, espacioGateway);
    }

    @Test
    void tieneRutaEspacio_esVerdaderoSiElEspacioTieneRuta() {
        when(rutaRepository.existsByEspacioOrigenId(espacioId)).thenReturn(true);

        assertThat(verificador.tieneRutaEspacio(espacioId)).isTrue();
    }

    @Test
    void tieneRutaEspacio_esFalsoSiNoTieneRuta() {
        when(rutaRepository.existsByEspacioOrigenId(espacioId)).thenReturn(false);

        assertThat(verificador.tieneRutaEspacio(espacioId)).isFalse();
    }

    @Test
    void tieneRutaZonaSegura_esVerdaderoSiAlgunaRutaLaTieneDeDestino() {
        when(rutaRepository.existsByZonaSeguraDestinoId(zonaId)).thenReturn(true);

        assertThat(verificador.tieneRutaZonaSegura(zonaId)).isTrue();
    }

    @Test
    void tieneRutaZonaSegura_esFalsoSiNingunaRutaLaUsa() {
        when(rutaRepository.existsByZonaSeguraDestinoId(zonaId)).thenReturn(false);

        assertThat(verificador.tieneRutaZonaSegura(zonaId)).isFalse();
    }

    @Test
    void tieneRutaEdificio_esFalsoSiElEdificioNoTieneEspacios() {
        when(espacioGateway.listarPorEdificio(edificioId)).thenReturn(List.of());

        assertThat(verificador.tieneRutaEdificio(edificioId)).isFalse();
        verify(rutaRepository, never()).existsByEspacioOrigenIdIn(anyCollection());
    }

    @Test
    void tieneRutaEdificio_esVerdaderoSiAlgunEspacioDelEdificioTieneRuta() {
        when(espacioGateway.listarPorEdificio(edificioId))
                .thenReturn(List.of(new EspacioInfo(espacioId, edificioId, "Salón 101", true)));
        when(rutaRepository.existsByEspacioOrigenIdIn(List.of(espacioId))).thenReturn(true);

        assertThat(verificador.tieneRutaEdificio(edificioId)).isTrue();
    }

    @Test
    void tieneRutaEdificio_esFalsoSiNingunEspacioTieneRuta() {
        when(espacioGateway.listarPorEdificio(edificioId))
                .thenReturn(List.of(new EspacioInfo(espacioId, edificioId, "Salón 101", true)));
        when(rutaRepository.existsByEspacioOrigenIdIn(List.of(espacioId))).thenReturn(false);

        assertThat(verificador.tieneRutaEdificio(edificioId)).isFalse();
    }
}
