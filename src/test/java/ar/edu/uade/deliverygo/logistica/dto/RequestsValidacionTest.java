package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// RN-01: verifica las anotaciones de Bean Validation de los requests.
// Usa el mismo Validator que Spring aplica cuando el controller recibe @Valid
class RequestsValidacionTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // ---------- DTO-01 RegistrarRepartidorRequest (CU-01) ----------

    @Test
    void unAltaCompletaNoTieneErrores() {
        assertThat(camposConError(altaValida())).isEmpty();
    }

    @Test
    void informaTodosLosCamposObligatoriosQueFaltanJuntos() {
        assertThat(camposConError(new RegistrarRepartidorRequest())).containsExactlyInAnyOrder(
                "usuarioId", "nombre", "apellido", "dni", "telefono", "vehiculo");
    }

    @Test
    void elDniConPuntosEsInvalido() {
        RegistrarRepartidorRequest alta = altaValida();
        alta.setDni("30.123.456");

        assertThat(camposConError(alta)).containsExactly("dni");
    }

    @Test
    void validaTambienElVehiculoAnidado() {
        RegistrarRepartidorRequest alta = altaValida();
        alta.setVehiculo(new VehiculoDTO(null, null, null));

        assertThat(camposConError(alta)).containsExactly("vehiculo.tipo");
    }

    // ---------- DTO-03 CambiarDisponibilidadRequest (CU-02) ----------

    @Test
    void laDisponibilidadEsObligatoria() {
        assertThat(camposConError(new CambiarDisponibilidadRequest())).containsExactly("disponibilidad");
    }

    @Test
    void unCambioDeDisponibilidadCompletoNoTieneErrores() {
        assertThat(camposConError(new CambiarDisponibilidadRequest(Disponibilidad.DISPONIBLE))).isEmpty();
    }

    // ---------- DTO-04 AsignarEntregaRequest (CU-03) ----------

    @Test
    void unaAsignacionCompletaNoTieneErroresYElRepartidorEsOpcional() {
        AsignarEntregaRequest request = new AsignarEntregaRequest(100L, 200L, 300L,
                "Av. Corrientes 1234, CABA", "Lima 757, CABA", null);

        assertThat(camposConError(request)).isEmpty();
    }

    @Test
    void laAsignacionInformaLosCamposObligatoriosQueFaltan() {
        assertThat(camposConError(new AsignarEntregaRequest())).containsExactlyInAnyOrder(
                "pedidoId", "clienteId", "comercioId", "direccionRetiro", "direccionEntrega");
    }

    // ---------- DTO-06 RegistrarRetiroRequest (CU-04) ----------

    @Test
    void elRetiroSinObservacionNoTieneErrores() {
        assertThat(camposConError(new RegistrarRetiroRequest())).isEmpty();
    }

    @Test
    void laObservacionDelRetiroNoPuedePasarDe255Caracteres() {
        assertThat(camposConError(new RegistrarRetiroRequest("x".repeat(256))))
                .containsExactly("observacion");
    }

    private Set<String> camposConError(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private RegistrarRepartidorRequest altaValida() {
        return new RegistrarRepartidorRequest(1L, "Juan", "Perez", "30123456", "1155554444",
                new VehiculoDTO(TipoVehiculo.MOTO, "AB123CD", null));
    }
}
