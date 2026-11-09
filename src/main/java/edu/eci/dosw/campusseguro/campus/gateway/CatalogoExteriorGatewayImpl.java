package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.CatalogoExterior;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CatalogoExteriorGatewayImpl implements CatalogoExterior {

    private final SalidaRepository salidaRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;
    private final ZonaSeguraRepository zonaSeguraRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalidaInfo> salidasDisponibles() {
        Set<UUID> edificiosActivos = edificioRepository.findAll().stream()
                .filter(e -> e.getEstado() == Estado.ACTIVO)
                .map(Edificio::getId)
                .collect(Collectors.toSet());
        Set<UUID> pisosActivos = pisoRepository.findAll().stream()
                .filter(p -> p.getEstado() == Estado.ACTIVO)
                .map(Piso::getId)
                .collect(Collectors.toSet());

        return salidaRepository.findAll().stream()
                .filter(s -> s.getEstado() == Estado.ACTIVO)
                .filter(s -> edificiosActivos.contains(s.getEdificioId()) && pisosActivos.contains(s.getPisoId()))
                .map(s -> new SalidaInfo(s.getId(), s.getEdificioId(), s.getPisoId(), s.getNombre(),
                        s.getLatitud(), s.getLongitud(), true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZonaInfo> zonasActivasConCoordenadas() {
        return zonaSeguraRepository.findAll().stream()
                .filter(z -> z.getEstado() == Estado.ACTIVO)
                .filter(z -> z.getLatitud() != null && z.getLongitud() != null)
                .map(z -> new ZonaInfo(z.getId(), z.getNombre(), z.getUbicacion(), true,
                        z.getLatitud(), z.getLongitud()))
                .toList();
    }
}
