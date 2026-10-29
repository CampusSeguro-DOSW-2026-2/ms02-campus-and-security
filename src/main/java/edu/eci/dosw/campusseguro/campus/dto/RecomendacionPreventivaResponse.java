package edu.eci.dosw.campusseguro.campus.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.AmbitoRecomendacion;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;

public record RecomendacionPreventivaResponse(
        UUID id,
        AmbitoRecomendacion ambito,
        UUID edificioId,
        TipoEspacio tipoEspacio,
        String titulo,
        String recomendacion,
        Estado estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
