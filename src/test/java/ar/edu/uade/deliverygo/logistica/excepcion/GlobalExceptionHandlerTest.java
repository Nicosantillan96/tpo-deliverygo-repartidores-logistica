package ar.edu.uade.deliverygo.logistica.excepcion;

import ar.edu.uade.deliverygo.logistica.dto.ErrorResponse;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

// DTO-08 y seccion 17: formato normalizado y codigo HTTP de cada error.
// Los errores de @Valid y del header X-Usuario-Id se prueban con los controllers
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unErrorDeNegocioUsaElFormatoNormalizado() {
        ResponseEntity<ErrorResponse> respuesta = handler.manejarReglaNegocio(
                new ReglaNegocioException(CodigoError.DELIVERY_NOT_FOUND, "No existe la entrega 9"),
                request("/api/v1/deliveries/9/pickup"));

        ErrorResponse error = respuesta.getBody();
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getCode()).isEqualTo("DELIVERY_NOT_FOUND");
        assertThat(error.getMessage()).isEqualTo("No existe la entrega 9");
        assertThat(error.getPath()).isEqualTo("/api/v1/deliveries/9/pickup");
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getCorrelationId()).isNotBlank();
    }

    // Seccion 17: un caso de cada grupo de codigos HTTP
    @Test
    void cadaCodigoDeNegocioTieneSuHttpSegunLaEspecificacion() {
        assertThat(status(CodigoError.VEHICLE_PLATE_REQUIRED)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status(CodigoError.COURIER_NOT_ASSIGNED)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(CodigoError.ORDER_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(CodigoError.DELIVERY_NOT_PICKED_UP)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(status(CodigoError.INVALID_CONFIRMATION_CODE))
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cadaErrorTieneUnCorrelationIdDistinto() {
        ReglaNegocioException ex = new ReglaNegocioException(CodigoError.FORBIDDEN, "No");

        String primero = handler.manejarReglaNegocio(ex, request("/x")).getBody().getCorrelationId();
        String segundo = handler.manejarReglaNegocio(ex, request("/x")).getBody().getCorrelationId();

        assertThat(primero).isNotEqualTo(segundo);
    }

    // DM-09: dos cambios simultaneos sobre el mismo repartidor
    @Test
    void unConflictoDeVersionDa409() {
        ResponseEntity<ErrorResponse> respuesta = handler.manejarConcurrencia(
                new ObjectOptimisticLockingFailureException(Repartidor.class, 1L),
                request("/api/v1/deliveries/assign"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().getCode()).isEqualTo("CONCURRENT_UPDATE");
    }

    private HttpStatus status(CodigoError codigo) {
        return (HttpStatus) handler.manejarReglaNegocio(
                new ReglaNegocioException(codigo, "mensaje"), request("/x")).getStatusCode();
    }

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }
}
