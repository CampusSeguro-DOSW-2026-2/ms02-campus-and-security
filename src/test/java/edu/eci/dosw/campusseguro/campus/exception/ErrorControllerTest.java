package edu.eci.dosw.campusseguro.campus.exception;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorControllerTest {

    private final ErrorController controller = new ErrorController();

    private ResponseEntity<ErrorResponse> conEstado(Object status) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.addHeader("X-Correlation-ID", "7b0c8f2e-3c1d-4f5a-9e6b-1a2b3c4d5e6f");
        return controller.error(request);
    }

    @Test
    void notFound_usaElCodigoNoEncontrado() {
        ResponseEntity<ErrorResponse> r = conEstado(404);

        assertThat(r.getStatusCode().value()).isEqualTo(404);
        assertThat(r.getBody().codigo()).isEqualTo("NO_ENCONTRADO");
        assertThat(r.getBody().errores()).isEmpty();
        assertThat(r.getBody().correlationId()).isEqualTo("7b0c8f2e-3c1d-4f5a-9e6b-1a2b3c4d5e6f");
    }

    @Test
    void metodoNoPermitido_usaSuCodigo() {
        assertThat(conEstado(405).getBody().codigo()).isEqualTo("METODO_NO_PERMITIDO");
    }

    @Test
    void sinEstadoOConError5xx_devuelveErrorInterno() {
        assertThat(conEstado(null).getStatusCode().value()).isEqualTo(500);
        assertThat(conEstado(503).getBody().codigo()).isEqualTo("ERROR_INTERNO");
    }
}
