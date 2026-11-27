package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalidaGatewayImpl implements SalidaGateway {

    private final SalidaRepository salidaRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;

    @Override
    public Optional<SalidaInfo> buscarPorId(UUID id) {
        return salidaRepository.findById(id)
                .map(s -> aInfo(s, edificioActivo(s.getEdificioId()) && pisoActivo(s.getPisoId())));
    }

    @Override
    public List<SalidaInfo> listarPorEdificio(UUID edificioId) {
        boolean edificioActivo = edificioActivo(edificioId);
        Set<UUID> pisosActivos = pisoRepository.findByEdificioIdOrderByNumeroAsc(edificioId).stream()
                .filter(p -> p.getEstado() == Estado.ACTIVO)
                .map(Piso::getId)
                .collect(Collectors.toSet());
        return salidaRepository.findByEdificioIdOrderByNombreAsc(edificioId).stream()
                .map(s -> aInfo(s, edificioActivo && pisosActivos.contains(s.getPisoId())))
                .toList();
    }

    private boolean edificioActivo(UUID edificioId) {
        return edificioRepository.findById(edificioId)
                .map(e -> e.getEstado() == Estado.ACTIVO)
                .orElse(false);
    }

    private boolean pisoActivo(UUID pisoId) {
        return pisoRepository.findById(pisoId)
                .map(p -> p.getEstado() == Estado.ACTIVO)
                .orElse(false);
    }

    private static SalidaInfo aInfo(Salida s, boolean contenedoresActivos) {
        return new SalidaInfo(s.getId(), s.getEdificioId(), s.getPisoId(), s.getNombre(),
                s.getLatitud(), s.getLongitud(), contenedoresActivos && s.getEstado() == Estado.ACTIVO);
    }
}
