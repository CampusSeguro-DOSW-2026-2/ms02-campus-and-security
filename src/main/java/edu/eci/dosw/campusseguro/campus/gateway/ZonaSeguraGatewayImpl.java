package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ZonaSeguraGatewayImpl implements ZonaSeguraGateway {

    private final ZonaSeguraRepository zonaSeguraRepository;

    @Override
    public Optional<ZonaInfo> buscarPorId(UUID id) {
        return zonaSeguraRepository.findById(id)
                .map(z -> new ZonaInfo(
                        z.getId(),
                        z.getNombre(),
                        z.getUbicacion(),
                        z.getEstado() == Estado.ACTIVO));
    }
}
