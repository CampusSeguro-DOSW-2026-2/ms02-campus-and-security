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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return espacioRepository.findById(id).map(e -> {
            Piso piso = pisoRepository.findById(e.getPisoId()).orElse(null);
            boolean pisoActivo = piso != null && piso.getEstado() == Estado.ACTIVO;
            return aInfo(e, edificioActivo(e.getEdificioId()) && pisoActivo,
                    piso == null ? null : piso.getNumero());
        });
    }

    @Override
    public List<EspacioInfo> listarPorEdificio(UUID edificioId) {
        boolean edificioActivo = edificioActivo(edificioId);
        List<Piso> pisos = pisoRepository.findByEdificioIdOrderByNumeroAsc(edificioId);
        Set<UUID> pisosActivos = pisos.stream()
                .filter(p -> p.getEstado() == Estado.ACTIVO)
                .map(Piso::getId)
                .collect(Collectors.toSet());
        // HashMap y no Collectors.toMap: el número del piso puede ser null y toMap lanzaría NullPointerException.
        Map<UUID, Integer> numeros = new HashMap<>();
        pisos.forEach(p -> numeros.put(p.getId(), p.getNumero()));
        return espacioRepository.findByEdificioId(edificioId).stream()
                .map(e -> aInfo(e, edificioActivo && pisosActivos.contains(e.getPisoId()),
                        numeros.get(e.getPisoId())))
                .toList();
    }

    private boolean edificioActivo(UUID edificioId) {
        return edificioRepository.findById(edificioId)
                .map(ed -> ed.getEstado() == Estado.ACTIVO)
                .orElse(false);
    }

    private static EspacioInfo aInfo(Espacio e, boolean contenedoresActivos, Integer pisoNumero) {
        return new EspacioInfo(
                e.getId(),
                e.getEdificioId(),
                e.getNombre(),
                contenedoresActivos && e.getEstado() == Estado.ACTIVO,
                e.getTipo(),
                e.getPisoId(),
                pisoNumero);
    }
}
