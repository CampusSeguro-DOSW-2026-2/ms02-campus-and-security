package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.entity.DisponibilidadEscalera;
import edu.eci.dosw.campusseguro.campus.entity.Escalera;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.repository.EscaleraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.EscaleraGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EscaleraGatewayImpl implements EscaleraGateway {

    private final EscaleraRepository escaleraRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EscaleraInfo> listarDisponiblesQueConectan(
            UUID edificioId, UUID pisoOrigenId, UUID pisoDestinoId) {

        return disponibles(edificioId).stream()
                .filter(e -> pisosDe(e).containsAll(Set.of(pisoOrigenId, pisoDestinoId)))
                .map(EscaleraGatewayImpl::aInfo)
                .toList();
    }

    // Busca el recorrido con menos tramos entre los dos pisos (una escalera puede cubrir varios pisos y
    // se pueden encadenar varias). Recorre las escaleras por prioridad, asi a igual numero de tramos
    // gana el camino de las escaleras principales.
    @Override
    @Transactional(readOnly = true)
    public List<List<EscaleraInfo>> tramosEntre(UUID edificioId, UUID pisoOrigenId, UUID pisoDestinoId) {
        if (pisoOrigenId.equals(pisoDestinoId)) {
            return List.of();
        }
        List<Escalera> escaleras = disponibles(edificioId);

        Map<UUID, UUID> anterior = new HashMap<>();
        Deque<UUID> cola = new ArrayDeque<>();
        anterior.put(pisoOrigenId, pisoOrigenId);
        cola.add(pisoOrigenId);

        while (!cola.isEmpty() && !anterior.containsKey(pisoDestinoId)) {
            UUID actual = cola.poll();
            for (Escalera escalera : escaleras) {
                Set<UUID> pisos = pisosDe(escalera);
                if (!pisos.contains(actual)) {
                    continue;
                }
                for (UUID siguiente : pisos) {
                    if (anterior.putIfAbsent(siguiente, actual) == null) {
                        cola.add(siguiente);
                    }
                }
            }
        }
        if (!anterior.containsKey(pisoDestinoId)) {
            return List.of();
        }

        Deque<UUID[]> saltos = new ArrayDeque<>();
        for (UUID piso = pisoDestinoId; !piso.equals(pisoOrigenId); piso = anterior.get(piso)) {
            saltos.addFirst(new UUID[] {anterior.get(piso), piso});
        }
        return saltos.stream()
                .map(salto -> escaleras.stream()
                        .filter(e -> pisosDe(e).containsAll(Set.of(salto[0], salto[1])))
                        .map(EscaleraGatewayImpl::aInfo)
                        .toList())
                .toList();
    }

    // Solo escaleras activas y disponibles (bloqueadas o fuera de servicio no se recomiendan), en orden
    // de prioridad y luego por codigo para que el resultado sea estable.
    private List<Escalera> disponibles(UUID edificioId) {
        return escaleraRepository.findByEdificioId(edificioId).stream()
                .filter(e -> e.getEstado() == Estado.ACTIVO)
                .filter(e -> e.getDisponibilidad() == DisponibilidadEscalera.DISPONIBLE)
                .sorted(Comparator.comparingInt(Escalera::getPrioridad).thenComparing(Escalera::getCodigo))
                .toList();
    }

    private static Set<UUID> pisosDe(Escalera escalera) {
        return escalera.getPisos().stream().map(Piso::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static EscaleraInfo aInfo(Escalera e) {
        return new EscaleraInfo(e.getId(), e.getCodigo(), e.getNombre(), e.getEdificioId(),
                e.getPisos().stream().map(Piso::getId).toList(), e.getPrioridad());
    }
}
