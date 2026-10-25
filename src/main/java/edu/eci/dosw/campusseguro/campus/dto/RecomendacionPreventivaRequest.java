package edu.eci.dosw.campusseguro.campus.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.AmbitoRecomendacion;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecomendacionPreventivaRequest(
        @NotNull(message = "El ámbito es obligatorio")
        AmbitoRecomendacion ambito,

        UUID edificioId,

        TipoEspacio tipoEspacio,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @NotBlank(message = "La recomendación es obligatoria")
        String recomendacion
) {
}
