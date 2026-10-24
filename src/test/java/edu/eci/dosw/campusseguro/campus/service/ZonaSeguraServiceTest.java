package edu.eci.dosw.campusseguro.campus.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.ZonaCodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZonaSeguraServiceTest {

    @Mock
    private ZonaSeguraRepository zonaSeguraRepository;

    @InjectMocks
    private ZonaSeguraService zonaSeguraService;

    private ZonaSeguraRequest request(String codigo) {
        return new ZonaSeguraRequest(codigo, "Punto H", "Frente al bloque H", "Zona abierta", 200);
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
    void modificar_conCodigoDeOtraZona_lanzaExcepcion() {
        UUID id = uuid(2);
        when(zonaSeguraRepository.findById(id)).thenReturn(Optional.of(new ZonaSegura()));
        when(zonaSeguraRepository.existsByCodigoAndIdNot("MP-H-01", id)).thenReturn(true);

        assertThatThrownBy(() -> zonaSeguraService.modificar(id, request("MP-H-01")))
                .isInstanceOf(ZonaCodigoDuplicadoException.class);

        verify(zonaSeguraRepository, never()).save(any());
    }
}