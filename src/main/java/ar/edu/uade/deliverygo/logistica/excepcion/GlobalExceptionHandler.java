package ar.edu.uade.deliverygo.logistica.excepcion;

import ar.edu.uade.deliverygo.logistica.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

// Traduce las excepciones al formato normalizado de error (DTO-08, seccion 17).
// Como en el ejemplo de clase: un @RestControllerAdvice con un @ExceptionHandler
// por tipo de excepcion. El codigo HTTP de cada error de negocio sale de CodigoError.
// Cada respuesta lleva un correlationId nuevo que tambien se escribe en el log,
// para poder encontrar el error a partir de lo que vio el cliente.
// Los errores no previstos (500) y los propios de Spring (URL inexistente 404,
// metodo no permitido 405) no se capturan aca: los responde Spring con su formato.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Errores de negocio lanzados por los services y las entidades
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(ReglaNegocioException ex,
                                                             HttpServletRequest request) {
        return responder(ex.getCodigo(), ex.getMessage(), request);
    }

    // RN-01: @Valid encontro campos invalidos en el request. Se informan todos juntos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return responder(CodigoError.VALIDATION_ERROR, mensaje, request);
    }

    // JSON mal formado o con un valor que no existe en un enum
    // (por ejemplo "disponibilidad": "DE_VACACIONES")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        return responder(CodigoError.VALIDATION_ERROR,
                "El cuerpo del pedido no es un JSON válido o tiene valores no permitidos", request);
    }

    // Un parametro de la URL con el tipo equivocado, por ejemplo /couriers/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        return responder(CodigoError.VALIDATION_ERROR,
                "El parámetro " + ex.getName() + " tiene un valor inválido", request);
    }

    // RN-10: falta el header que identifica al usuario (X-Usuario-Id)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> manejarHeaderFaltante(MissingRequestHeaderException ex,
                                                               HttpServletRequest request) {
        return responder(CodigoError.VALIDATION_ERROR,
                "Falta el header " + ex.getHeaderName(), request);
    }

    // DM-09: @Version detecto que otra operacion modifico el repartidor al mismo tiempo
    // (por ejemplo, dos asignaciones simultaneas). Quien llama puede reintentar
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> manejarConcurrencia(ObjectOptimisticLockingFailureException ex,
                                                             HttpServletRequest request) {
        return responder(CodigoError.CONCURRENT_UPDATE,
                "Los datos fueron modificados por otra operación. Intente nuevamente", request);
    }

    // Arma el ErrorResponse con un correlationId nuevo y lo deja tambien en el log
    private ResponseEntity<ErrorResponse> responder(CodigoError codigo, String mensaje,
                                                    HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] {} {} en {}: {}", correlationId, codigo.getStatus().value(), codigo,
                request.getRequestURI(), mensaje);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                codigo.getStatus().value(),
                codigo.name(),
                mensaje,
                request.getRequestURI(),
                correlationId);
        return ResponseEntity.status(codigo.getStatus()).body(error);
    }
}
