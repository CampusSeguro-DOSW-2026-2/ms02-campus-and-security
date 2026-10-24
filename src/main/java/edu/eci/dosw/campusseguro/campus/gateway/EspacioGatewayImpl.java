package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EspacioGatewayImpl implements EspacioGateway {

    private final EspacioRepository espacioRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;

    @Override
    public Optional<EspacioInfo> buscarPorId(UUID id) {
        return espacioRepository.findById(id)
                .map(e -> aInfo(e, edificioActivo(e.getEdificioId()) && pisoActivo(e.getPisoId())));
    }

    @Override
    public List<EspacioInfo> listarPorEdificio(UUID edificioId) {
        boolean edificioActivo = edificioActivo(edificioId);
        Set<UUID> pisosActivos = pisoRepository.findByEdificioIdOrderByNumeroAsc(edificioId).stream()
                .filter(p -> p.getEstado() == Estado.ACTIVO)
                .map(Piso::getId)
                .collect(Collectors.toSet());
        return espacioRepository.findByEdificioId(edificioId).stream()
                .map(e -> aInfo(e, edificioActivo && pisosActivos.contains(e.getPisoId())))
                .toList();
    }

    private boolean edificioActivo(UUID edificioId) {
        return edificioRepository.findById(edificioId)
                .map(ed -> ed.getEstado() == Estado.ACTIVO)
                .orElse(false);
    }

    private boolean pisoActivo(UUID pisoId) {
        return pisoRepository.findById(pisoId)
                .map(p -> p.getEstado() == Estado.ACTIVO)
                .orElse(false);
    }

    private static EspacioInfo aInfo(Espacio e, boolean contenedoresActivos) {
        return new EspacioInfo(
                e.getId(),
                e.getEdificioId(),
                e.getNombre(),
                contenedoresActivos && e.getEstado() == Estado.ACTIVO);
    }
}
