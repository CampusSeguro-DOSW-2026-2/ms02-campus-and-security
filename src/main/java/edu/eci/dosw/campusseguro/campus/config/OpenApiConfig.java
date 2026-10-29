package edu.eci.dosw.campusseguro.campus.config;

import edu.eci.dosw.campusseguro.campus.exception.ErrorResponse;
import edu.eci.dosw.campusseguro.campus.security.HeaderAuthenticationFilter;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.RolPermisos;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String JSON = "application/json";
    private static final String PARAMETROS = "#/components/parameters/";
    private static final String RESPUESTAS = "#/components/responses/";
    private static final String CABECERA_CORRELATION = "#/components/headers/XCorrelationID";
    private static final String CORRELATION_EJEMPLO = "7b0c8f2e-3c1d-4f5a-9e6b-1a2b3c4d5e6f";

    @Bean
    public OpenAPI campusOpenApi() {
        Components components = new Components()
                .addHeaders("XCorrelationID", new Header()
                        .description("UUID v4 que enlaza logs y eventos de auditoría. Si no llega, MS02 lo genera.")
                        .schema(new StringSchema().format("uuid").example(CORRELATION_EJEMPLO)))
                .addParameters("XUserId", cabecera(HeaderAuthenticationFilter.USER_ID, true,
                        "Id del usuario autenticado. Lo pone el API Gateway.", "USR-1034"))
                .addParameters("XUserRole", cabecera(HeaderAuthenticationFilter.USER_ROLE, true,
                        "Roles del usuario autenticado separados por coma. Lo pone el API Gateway.",
                        "ESTUDIANTE,BRIGADISTA"))
                .addParameters("XCorrelationId", cabecera(CorrelationIdFilter.HEADER, false,
                        "Identificador de trazabilidad de la petición (UUID v4).", CORRELATION_EJEMPLO))
                .addResponses("BadRequest", error("Validación fallida: indica el campo y la regla.",
                        400, "VALIDACION", "Datos inválidos.",
                        List.of(new ErrorResponse.CampoError("nombre", "El nombre es obligatorio."))))
                .addResponses("Unauthorized", error("Faltan las cabeceras de identidad (X-User-Id / X-User-Role).",
                        401, "NO_AUTENTICADO", "Faltan las cabeceras de identidad.", List.of()))
                .addResponses("Forbidden", error("X-User-Role no incluye ningún rol de x-roles.",
                        403, "PROHIBIDO", "No tiene permiso para realizar esta acción.", List.of()))
                .addResponses("NotFound", error("El recurso no existe.",
                        404, "NO_ENCONTRADO", "Edificio no encontrado.", List.of()))
                .addResponses("InternalError", error("Error inesperado; el detalle queda en el log de MS02.",
                        500, "ERROR_INTERNO", "Ocurrió un error inesperado. Intente de nuevo más tarde.", List.of()));
        ModelConverters.getInstance().readAll(ErrorResponse.class).forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("MS02 Campus y seguridad")
                        .version("v1")
                        .description("Edificios, pisos, espacios, zonas seguras y rutas de evacuación de CampusSeguro.\n\n"
                                + "**Entradas de identidad:** los endpoints protegidos reciben `X-User-Id` y "
                                + "`X-User-Role` (roles separados por coma), que pone el API Gateway; todos aceptan "
                                + "`X-Correlation-ID`. Si faltan las de identidad, MS02 responde 401. "
                                + "Cada operación declara su permiso en `x-permiso` y los roles que lo tienen en `x-roles`."))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Acceso externo (API Gateway)"),
                        new Server().url("http://localhost:8082").description("MS02 directo, solo desarrollo")))
                .components(components);
    }

    @Bean
    public OperationCustomizer estandarDeIntegracion() {
        return (operation, metodo) -> {
            Permiso permiso = metodo.getMethodAnnotation(Permiso.class);
            if (permiso != null) {
                operation.addExtension("x-permiso", permiso.value());
                operation.addExtension("x-roles", RolPermisos.rolesCon(permiso.value()));
            }
            cabeceras(operation, permiso != null);
            respuestas(operation, metodo, permiso != null);
            return operation;
        };
    }

    private static void cabeceras(Operation operation, boolean protegido) {
        List<Parameter> parametros = new ArrayList<>();
        if (protegido) {
            parametros.add(new Parameter().$ref(PARAMETROS + "XUserId"));
            parametros.add(new Parameter().$ref(PARAMETROS + "XUserRole"));
        }
        parametros.add(new Parameter().$ref(PARAMETROS + "XCorrelationId"));
        if (operation.getParameters() != null) {
            parametros.addAll(operation.getParameters());
        }
        operation.setParameters(parametros);
    }

    private static void respuestas(Operation operation, HandlerMethod metodo, boolean protegido) {
        ApiResponses respuestas = new ApiResponses();
        boolean crea = metodo.hasMethodAnnotation(PostMapping.class);
        respuestas.addApiResponse(crea ? "201" : "200", new ApiResponse()
                .description(crea ? "Creado." : "OK.")
                .content(json(esquemaDe(metodo))));
        if (tiene(metodo, PathVariable.class, RequestParam.class, RequestBody.class)) {
            respuestas.addApiResponse("400", new ApiResponse().$ref(RESPUESTAS + "BadRequest"));
        }
        if (protegido) {
            respuestas.addApiResponse("401", new ApiResponse().$ref(RESPUESTAS + "Unauthorized"));
            respuestas.addApiResponse("403", new ApiResponse().$ref(RESPUESTAS + "Forbidden"));
        }
        if (tiene(metodo, PathVariable.class)) {
            respuestas.addApiResponse("404", new ApiResponse().$ref(RESPUESTAS + "NotFound"));
        }
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((codigo, declarada) -> {
                if (codigo.matches("[45]\\d\\d")) {
                    respuestas.addApiResponse(codigo, declarada.content(json(esquemaError())));
                }
            });
        }
        respuestas.addApiResponse("500", new ApiResponse().$ref(RESPUESTAS + "InternalError"));
        respuestas.values().stream()
                .filter(r -> r.get$ref() == null)
                .forEach(r -> r.addHeaderObject(CorrelationIdFilter.HEADER, new Header().$ref(CABECERA_CORRELATION)));
        operation.setResponses(respuestas);
    }

    @SafeVarargs
    private static boolean tiene(HandlerMethod metodo, Class<? extends java.lang.annotation.Annotation>... anotaciones) {
        return Arrays.stream(metodo.getMethodParameters())
                .anyMatch(p -> Arrays.stream(anotaciones).anyMatch(p::hasParameterAnnotation));
    }

    private static Schema<?> esquemaDe(HandlerMethod metodo) {
        ResolvableType tipo = ResolvableType.forMethodReturnType(metodo.getMethod());
        if (ResponseEntity.class.equals(tipo.toClass())) {
            tipo = tipo.getGeneric(0);
        }
        if (List.class.isAssignableFrom(tipo.toClass())) {
            return new ArraySchema().items(referencia(tipo.getGeneric(0).toClass()));
        }
        return referencia(tipo.toClass());
    }

    private static Schema<?> referencia(Class<?> clase) {
        return new Schema<>().$ref("#/components/schemas/" + clase.getSimpleName());
    }

    private static Schema<?> esquemaError() {
        return referencia(ErrorResponse.class);
    }

    private static Content json(Schema<?> esquema) {
        return new Content().addMediaType(JSON, new MediaType().schema(esquema));
    }

    private static ApiResponse error(String descripcion, int status, String codigo, String mensaje,
                                     List<ErrorResponse.CampoError> errores) {
        return new ApiResponse()
                .description(descripcion)
                .addHeaderObject(CorrelationIdFilter.HEADER, new Header().$ref(CABECERA_CORRELATION))
                .content(new Content().addMediaType(JSON, new MediaType()
                        .schema(esquemaError())
                        .example(new ErrorResponse(status, codigo, mensaje, errores, CORRELATION_EJEMPLO))));
    }

    private static Parameter cabecera(String nombre, boolean obligatoria, String descripcion, String ejemplo) {
        return new Parameter()
                .in("header")
                .name(nombre)
                .required(obligatoria)
                .description(descripcion)
                .schema(new StringSchema().example(ejemplo));
    }
}
