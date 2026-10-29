package edu.eci.dosw.campusseguro.campus.exception;

import edu.eci.dosw.campusseguro.campus.config.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@RestController
public class ErrorController implements org.springframework.boot.webmvc.error.ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        HttpStatus status = estado(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        ErrorResponse cuerpo = new ErrorResponse(status.value(), codigo(status), mensaje(status), List.of(),
                CorrelationIdFilter.de(request));
        return ResponseEntity.status(status).body(cuerpo);
    }

    private static HttpStatus estado(Object codigo) {
        HttpStatus status = codigo instanceof Integer valor ? HttpStatus.resolve(valor) : null;
        return status == null || status.is5xxServerError() ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    private static String codigo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "VALIDACION";
            case UNAUTHORIZED -> "NO_AUTENTICADO";
            case FORBIDDEN -> "PROHIBIDO";
            case NOT_FOUND -> "NO_ENCONTRADO";
            case METHOD_NOT_ALLOWED -> "METODO_NO_PERMITIDO";
            case CONFLICT -> "CONFLICTO";
            case UNPROCESSABLE_CONTENT -> "REGLA_NEGOCIO";
            default -> "ERROR_INTERNO";
        };
    }

    private static String mensaje(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case METHOD_NOT_ALLOWED -> "El método no está permitido en esta ruta.";
            case INTERNAL_SERVER_ERROR -> ManejadorGlobalErrores.ERROR_INTERNO;
            default -> "La solicitud no se pudo procesar.";
        };
    }
}
