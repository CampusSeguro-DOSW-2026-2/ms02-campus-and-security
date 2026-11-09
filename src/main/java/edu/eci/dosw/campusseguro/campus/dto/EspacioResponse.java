package edu.eci.dosw.campusseguro.campus.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;

public record EspacioResponse(
        UUID id,
        UUID edificioId,
        String edificioNombre,
        UUID pisoId,
        Integer pisoNumero,
        String nombre,
        TipoEspacio tipo,
        Estado estado,
        boolean disponible,
        boolean tieneFotografia
) {
}
