package edu.eci.dosw.campusseguro.campus.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Formato de error comun del curso (mismo de MS01): status, codigo, mensaje, errores y correlation_id.
@Schema(type = "object", requiredProperties = {"status", "codigo", "mensaje", "errores", "correlation_id"})
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        List<CampoError> errores,
        @JsonProperty("correlation_id")
        String correlationId) {

    public record CampoError(String campo, String mensaje) {
    }
}
