package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.DisponibilidadEscalera;
import edu.eci.dosw.campusseguro.campus.entity.Escalera;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.repository.EscaleraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.EscaleraGateway.EscaleraInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Recorrido de escaleras entre pisos: prioridad, disponibilidad y escaleras encadenadas.
@ExtendWith(MockitoExtension.class)
class EscaleraGatewayImplTest {

    private final UUID edificioId = uuid(10);
    private final Piso piso1 = piso(21, 1);
    private final Piso piso2 = piso(22, 2);
    private final Piso piso3 = piso(23, 3);

    @Mock
    private EscaleraRepository escaleraRepository;
    @InjectMocks
    private EscaleraGatewayImpl gateway;

    private static Piso piso(int n, int numero) {
        Piso p = new Piso();
        p.setId(uuid(n));
        p.setNumero(numero);
        return p;
    }

    private Escalera escalera(String codigo, int prioridad, Piso... pisos) {
        Escalera e = new Escalera();
        e.setId(UUID.nameUUIDFromBytes(codigo.getBytes()));
        e.setEdificioId(edificioId);
        e.setCodigo(codigo);
        e.setNombre("Escalera " + codigo);
        e.setPrioridad(prioridad);
        e.setPisos(new HashSet<>(List.of(pisos)));
        return e;
    }

    @Test
    void tramosEntre_ordenaLasEscalerasPorPrioridad() {
        when(escaleraRepository.findByEdificioId(edificioId)).thenReturn(List.of(
                escalera("B-SUR", 2, piso1, piso3), escalera("A-NORTE", 1, piso1, piso3)));

        List<List<EscaleraInfo>> tramos = gateway.tramosEntre(edificioId, piso3.getId(), piso1.getId());

        assertThat(tramos).hasSize(1);
        assertThat(tramos.get(0)).extracting(EscaleraInfo::codigo).containsExactly("A-NORTE", "B-SUR");
    }

    @Test
    void tramosEntre_ignoraEscalerasBloqueadasOInactivas() {
        Escalera bloqueada = escalera("A", 1, piso1, piso3);
        bloqueada.setDisponibilidad(DisponibilidadEscalera.BLOQUEADA);
        Escalera inactiva = escalera("B", 2, piso1, piso3);
        inactiva.setEstado(Estado.INACTIVO);
        when(escaleraRepository.findByEdificioId(edificioId))
                .thenReturn(List.of(bloqueada, inactiva, escalera("C", 3, piso1, piso3)));

        List<List<EscaleraInfo>> tramos = gateway.tramosEntre(edificioId, piso3.getId(), piso1.getId());

        assertThat(tramos.get(0)).extracting(EscaleraInfo::codigo).containsExactly("C");
    }

    @Test
    void tramosEntre_encadenaEscalerasQueNoCubrenTodoElRecorrido() {
        when(escaleraRepository.findByEdificioId(edificioId)).thenReturn(List.of(
                escalera("ALTA", 1, piso2, piso3), escalera("BAJA", 1, piso1, piso2)));

        List<List<EscaleraInfo>> tramos = gateway.tramosEntre(edificioId, piso3.getId(), piso1.getId());

        assertThat(tramos).hasSize(2);
        assertThat(tramos.get(0)).extracting(EscaleraInfo::codigo).containsExactly("ALTA");
        assertThat(tramos.get(1)).extracting(EscaleraInfo::codigo).containsExactly("BAJA");
    }

    @Test
    void tramosEntre_prefiereUnaEscaleraQueCubreTodoAEncadenar() {
        when(escaleraRepository.findByEdificioId(edificioId)).thenReturn(List.of(
                escalera("ALTA", 1, piso2, piso3), escalera("BAJA", 1, piso1, piso2),
                escalera("DIRECTA", 2, piso1, piso2, piso3)));

        List<List<EscaleraInfo>> tramos = gateway.tramosEntre(edificioId, piso3.getId(), piso1.getId());

        assertThat(tramos).hasSize(1);
        assertThat(tramos.get(0)).extracting(EscaleraInfo::codigo).containsExactly("DIRECTA");
    }

    @Test
    void tramosEntre_sinEscalerasQueLleguen_devuelveVacio() {
        when(escaleraRepository.findByEdificioId(edificioId))
                .thenReturn(List.of(escalera("ALTA", 1, piso2, piso3)));

        assertThat(gateway.tramosEntre(edificioId, piso3.getId(), piso1.getId())).isEmpty();
    }

    @Test
    void tramosEntre_mismoPiso_devuelveVacio() {
        assertThat(gateway.tramosEntre(edificioId, piso3.getId(), piso3.getId())).isEmpty();
    }
}
