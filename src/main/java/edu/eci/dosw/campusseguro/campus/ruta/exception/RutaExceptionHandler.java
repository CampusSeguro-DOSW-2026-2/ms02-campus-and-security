package edu.eci.dosw.campusseguro.campus.ruta.exception;

import edu.eci.dosw.campusseguro.campus.ruta.controller.ConsultaRutaController;
import edu.eci.dosw.campusseguro.campus.ruta.controller.RutaController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Limitado a los controllers de rutas para no chocar con el manejador global del servicio.
@RestControllerAdvice(assignableTypes = {RutaController.class, ConsultaRutaController.class})
public class RutaExceptionHandler {

    public record ApiError(int status, String error, String mensaje,
                           Map<String, String> campos, Instant fecha) {
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), Map.of());
    }

    @ExceptionHandler(RutaDuplicadaException.class)
    public ResponseEntity<ApiError> duplicada(RutaDuplicadaException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos", campos);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos",
                Map.of(e.getName(), "Valor con formato inválido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroFaltante(MissingServletRequestParameterException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos",
                Map.of(e.getParameterName(), "El parámetro es obligatorio"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud es inválido o tiene un valor no permitido", Map.of());
    }

    private ResponseEntity<ApiError> respuesta(HttpStatus status, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), mensaje, campos, Instant.now()));
    }
}
