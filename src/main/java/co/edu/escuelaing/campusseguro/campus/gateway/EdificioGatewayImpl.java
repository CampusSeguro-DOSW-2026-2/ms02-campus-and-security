package co.edu.escuelaing.campusseguro.campus.gateway;

import co.edu.escuelaing.campusseguro.campus.entity.Estado;
import co.edu.escuelaing.campusseguro.campus.repository.EdificioRepository;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EdificioGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EdificioGatewayImpl implements EdificioGateway {

    private final EdificioRepository edificioRepository;

    @Override
    public Optional<EdificioInfo> buscarPorId(Long id) {
        return edificioRepository.findById(id)
                .map(e -> new EdificioInfo(
                        e.getId(),
                        e.getCodigo(),
                        e.getNombre(),
                        e.getEstado() == Estado.ACTIVO));
    }
}