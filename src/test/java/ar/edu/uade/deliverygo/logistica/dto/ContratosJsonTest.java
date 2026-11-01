package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Verifica como se ven los DTO en JSON con el mismo ObjectMapper que usa la API
@JsonTest
class ContratosJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    // DTO-05: nunca expone el codigo de confirmacion (RN-13)
    @Test
    void entregaResponseNoIncluyeElCodigoDeConfirmacion() throws Exception {
        EntregaResponse response = new EntregaResponse(
                1L, 100L, 7L, EstadoEntrega.ASIGNADA,
                LocalDateTime.of(2026, 10, 5, 19, 2, 11),
                LocalDateTime.of(2026, 10, 5, 19, 2, 11),
                null, null);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.has("codigoConfirmacion")).isFalse();
        assertThat(json.get("estado").asText()).isEqualTo("ASIGNADA");
        assertThat(json.get("repartidorId").asLong()).isEqualTo(7L);
        assertThat(json.get("fechaAsignacion").asText()).isEqualTo("2026-10-05T19:02:11");
        assertThat(json.get("fechaRetiro").isNull()).isTrue();
    }

    // DTO-01 con DTO-09 anidado
    @Test
    void registrarRepartidorRequestLeeElVehiculoAnidado() throws Exception {
        String json = """
                {
                  "usuarioId": 10,
                  "nombre": "Juan",
                  "apellido": "Perez",
                  "dni": "30123456",
                  "telefono": "1155554444",
                  "vehiculo": { "tipo": "BICICLETA", "descripcion": "Rodado 29" }
                }
                """;

        RegistrarRepartidorRequest request = objectMapper.readValue(json, RegistrarRepartidorRequest.class);

        assertThat(request.getUsuarioId()).isEqualTo(10L);
        assertThat(request.getVehiculo().getTipo()).isEqualTo(TipoVehiculo.BICICLETA);
        assertThat(request.getVehiculo().getPatente()).isNull();
    }

    // DTO-02: la respuesta incluye dni y telefono (decision del 01/11/2026)
    @Test
    void repartidorResponseIncluyeDniYTelefono() throws Exception {
        RepartidorResponse response = new RepartidorResponse(
                5L, 1L, "Juan", "Perez", "30123456", "1155554444",
                EstadoRepartidor.ACTIVO, Disponibilidad.NO_DISPONIBLE,
                new VehiculoDTO(TipoVehiculo.MOTO, "AB123CD", null),
                LocalDateTime.of(2026, 11, 1, 12, 0));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("dni").asText()).isEqualTo("30123456");
        assertThat(json.get("telefono").asText()).isEqualTo("1155554444");
        assertThat(json.get("estado").asText()).isEqualTo("ACTIVO");
        assertThat(json.get("vehiculo").get("patente").asText()).isEqualTo("AB123CD");
    }

    // DTO-03: un valor que no existe en el enum se rechaza al leer el JSON
    @Test
    void cambiarDisponibilidadConValorInexistenteFalla() {
        String json = """
                { "disponibilidad": "DE_VACACIONES" }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, CambiarDisponibilidadRequest.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    // DTO-07 (RN-10): si el cliente manda repartidorId, se ignora porque
    // el DTO no tiene ese campo; la identidad sale de la autenticacion
    @Test
    void confirmarEntregaIgnoraUnRepartidorIdEnviadoPorElCliente() throws Exception {
        String json = """
                { "codigoConfirmacion": "4821", "repartidorId": 99 }
                """;

        ConfirmarEntregaRequest request = objectMapper.readValue(json, ConfirmarEntregaRequest.class);

        assertThat(request.getCodigoConfirmacion()).isEqualTo("4821");
        assertThat(request.getObservacion()).isNull();
    }

    // DTO-08: nombres de campos del formato normalizado
    @Test
    void errorResponseUsaLosCamposDelFormatoNormalizado() throws Exception {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.of(2026, 10, 29, 12, 0), 409,
                "DELIVERY_NOT_PICKED_UP", "La entrega todavía no fue retirada",
                "/api/v1/deliveries/1/complete", "abc-123");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(error));

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "timestamp", "status", "code", "message", "path", "correlationId");
        assertThat(json.get("code").asText()).isEqualTo("DELIVERY_NOT_PICKED_UP");
    }
}
