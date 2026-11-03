package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.ConfirmarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.ErrorResponse;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRetiroRequest;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.service.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static ar.edu.uade.deliverygo.logistica.controller.RepartidorController.HEADER_USUARIO;

// API-03, API-04 y API-05. Solo recibe el pedido, valida el request (@Valid) y delega
// en el service. Los errores los traduce GlobalExceptionHandler.
// Las anotaciones @Tag, @Operation, @ApiResponse y @Parameter solo documentan la API
// (Swagger UI y el contrato OpenAPI en /v3/api-docs.yaml); no cambian lo que hace el codigo
@RestController
@RequestMapping("/api/v1/deliveries")
@Tag(name = "Entregas", description = "Asignación, retiro y confirmación de entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    // API-03 / CU-03: asignar repartidor -> 201 si quedo ASIGNADA,
    // 202 si no habia repartidores y quedo PENDIENTE_ASIGNACION (DM-11)
    // POST /api/v1/deliveries/assign
    // { "pedidoId": 100, "clienteId": 200, "comercioId": 300,
    //   "direccionRetiro": "Av. Corrientes 1234, CABA", "direccionEntrega": "Lima 757, CABA" }
    @Operation(summary = "CU-03 Asignar repartidor a una entrega",
            description = "Crea la entrega de un pedido confirmado, o reutiliza la que quedó pendiente, "
                    + "y le asigna el repartidor disponible que hace más tiempo no recibe una entrega. "
                    + "Con repartidorId se asigna ese repartidor en particular. "
                    + "El código de confirmación se envía al cliente y nunca sale en la respuesta.")
    @ApiResponse(responseCode = "201", description = "Entrega ASIGNADA a un repartidor")
    @ApiResponse(responseCode = "202", description = "No hay repartidores disponibles: la entrega queda "
            + "PENDIENTE_ASIGNACION. Al reintentar con el mismo pedidoId se reutiliza la misma entrega")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: faltan datos o tienen formato inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND: el pedido no existe. "
            + "COURIER_NOT_FOUND: el repartidor indicado no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "DELIVERY_ALREADY_EXISTS: el pedido ya tiene una entrega "
            + "asignada. CONCURRENT_UPDATE: otra operación tomó al mismo repartidor; se puede reintentar",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "ORDER_NOT_CONFIRMED: el pedido no está confirmado. "
            + "COURIER_NOT_AVAILABLE: el repartidor indicado no está activo y disponible",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/assign")
    public ResponseEntity<EntregaResponse> asignar(@Valid @RequestBody AsignarEntregaRequest request) {

        EntregaResponse entrega = entregaService.asignarRepartidor(request);
        HttpStatus status = entrega.getEstado() == EstadoEntrega.ASIGNADA
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(entrega);
    }

    // API-04 / CU-04: registrar retiro -> 200
    // POST /api/v1/deliveries/{id}/pickup   (header X-Usuario-Id: 1)
    // { "observacion": "Retirado en mostrador" }   (el body es opcional)
    @Operation(summary = "CU-04 Registrar retiro del pedido",
            description = "El repartidor asignado informa que retiró el pedido del comercio: "
                    + "ASIGNADA pasa a RETIRADA. El body con la observación es opcional.")
    @ApiResponse(responseCode = "200", description = "Entrega RETIRADA")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: observación demasiado larga "
            + "o falta el header X-Usuario-Id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "COURIER_NOT_ASSIGNED: el usuario no es el repartidor asignado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "DELIVERY_NOT_FOUND: la entrega no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "DELIVERY_INVALID_STATE: la entrega no está ASIGNADA",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/pickup")
    public ResponseEntity<EntregaResponse> registrarRetiro(
            @Parameter(description = "Id de la entrega", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody(required = false) RegistrarRetiroRequest request) {

        RegistrarRetiroRequest retiro = Objects.isNull(request) ? new RegistrarRetiroRequest() : request;
        return ResponseEntity.ok(entregaService.registrarRetiro(id, retiro, usuarioId));
    }

    // API-05 / CU-05: confirmar entrega -> 200
    // POST /api/v1/deliveries/{id}/complete   (header X-Usuario-Id: 1)
    // { "codigoConfirmacion": "4821" }
    @Operation(summary = "CU-05 Confirmar entrega del pedido",
            description = "El repartidor asignado confirma la entrega con el código que le da el cliente. "
                    + "La entrega pasa a COMPLETADA y el repartidor vuelve a DISPONIBLE.")
    @ApiResponse(responseCode = "200", description = "Entrega COMPLETADA")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: falta el código de confirmación "
            + "o el header X-Usuario-Id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "COURIER_NOT_ASSIGNED: el usuario no es el repartidor asignado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "DELIVERY_NOT_FOUND: la entrega no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "DELIVERY_NOT_PICKED_UP: la entrega todavía no fue retirada. "
            + "DELIVERY_INVALID_STATE: la entrega ya está completada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_CONFIRMATION_CODE: el código es incorrecto; "
            + "la entrega no cambia",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/complete")
    public ResponseEntity<EntregaResponse> confirmarEntrega(
            @Parameter(description = "Id de la entrega", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody ConfirmarEntregaRequest request) {

        return ResponseEntity.ok(entregaService.confirmarEntrega(id, request, usuarioId));
    }
}
