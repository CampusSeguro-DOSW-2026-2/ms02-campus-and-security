package edu.eci.dosw.campusseguro.campus.exception;

import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaDuplicadaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Unico manejador de errores de MS02. Todas las respuestas de error salen con el formato ErrorResponse.
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final String CORRELATION_ID = "X-Correlation-ID";
    private static final String DATOS_INVALIDOS = "Datos inválidos.";

    @ExceptionHandler({EdificioNoEncontradoException.class,
            ZonaSeguraNoEncontradaException.class,
            PisoNoEncontradoException.class,
            RecursoNoEncontradoException.class})
    public ResponseEntity<ErrorResponse> noEncontrado(RuntimeException e, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage(), List.of(), request);
    }

    @ExceptionHandler(CodigoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> codigoDuplicado(CodigoDuplicadoException e, HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores =
                List.of(new ErrorResponse.CampoError("codigo", "Ya existe un edificio con ese código."));
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage(), errores, request);
    }

    @ExceptionHandler(PisoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> pisoDuplicado(PisoDuplicadoException e, HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores =
                List.of(new ErrorResponse.CampoError("numero", "Ya existe un piso con ese número en el edificio."));
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage(), errores, request);
    }

    @ExceptionHandler(RutaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> rutaDuplicada(RutaDuplicadaException e, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException e, HttpServletRequest request) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "REGLA_NEGOCIO", e.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> porCampo = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> porCampo.putIfAbsent(f.getField(), f.getDefaultMessage()));
        List<ErrorResponse.CampoError> errores = porCampo.entrySet().stream()
                .map(c -> new ErrorResponse.CampoError(c.getKey(), c.getValue()))
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", DATOS_INVALIDOS, errores, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores =
                List.of(new ErrorResponse.CampoError(e.getName(), "Valor con formato inválido."));
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", DATOS_INVALIDOS, errores, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException e,
                                                           HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores =
                List.of(new ErrorResponse.CampoError(e.getParameterName(), "El parámetro es obligatorio."));
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", DATOS_INVALIDOS, errores, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION",
                "El cuerpo de la solicitud es inválido o tiene un valor no permitido.", List.of(), request);
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String codigo, String mensaje,
                                                    List<ErrorResponse.CampoError> errores,
                                                    HttpServletRequest request) {
        ErrorResponse cuerpo = new ErrorResponse(status.value(), codigo, mensaje, errores,
                request.getHeader(CORRELATION_ID));
        return ResponseEntity.status(status).body(cuerpo);
    }
}
