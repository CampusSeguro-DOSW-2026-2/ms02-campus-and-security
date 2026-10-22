package edu.eci.dosw.campusseguro.campus.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Formato de error comun del curso (mismo de MS01): status, codigo, mensaje, errores y correlation_id.
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        List<CampoError> errores,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("correlation_id")
        String correlationId) {

    public record CampoError(String campo, String mensaje) {
    }
}
