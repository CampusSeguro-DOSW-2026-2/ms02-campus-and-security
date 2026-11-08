package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.entity.DisponibilidadEscalera;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
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

        return escaleraRepository.findByEdificioId(edificioId).stream()
                .filter(e -> e.getEstado() == Estado.ACTIVO)
                .filter(e -> e.getDisponibilidad() == DisponibilidadEscalera.DISPONIBLE)
                .filter(e -> {
                    Set<UUID> pisos = e.getPisos().stream()
                            .map(p -> p.getId())
                            .collect(Collectors.toSet());
                    return pisos.contains(pisoOrigenId) && pisos.contains(pisoDestinoId);
                })
                .map(e -> new EscaleraInfo(
                        e.getId(),
                        e.getCodigo(),
                        e.getNombre(),
                        e.getEdificioId(),
                        e.getPisos().stream().map(p -> p.getId()).toList()))
                .toList();
    }
}
