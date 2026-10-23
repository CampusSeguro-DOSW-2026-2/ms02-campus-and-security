package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EspacioGatewayImpl implements EspacioGateway {

    private final EspacioRepository espacioRepository;

    @Override
    public Optional<EspacioInfo> buscarPorId(UUID id) {
        return espacioRepository.findById(id).map(EspacioGatewayImpl::aInfo);
    }

    @Override
    public List<EspacioInfo> listarPorEdificio(UUID edificioId) {
        return espacioRepository.findByEdificioId(edificioId).stream()
                .map(EspacioGatewayImpl::aInfo)
                .toList();
    }

    private static EspacioInfo aInfo(Espacio e) {
        return new EspacioInfo(
                e.getId(),
                e.getEdificioId(),
                e.getNombre(),
                e.getEstado() == Estado.ACTIVO);
    }
}
