package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.AgregarVehiculoRequest;
import ar.edu.uade.deliverygo.logistica.dto.AsignarZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.ErrorResponse;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoResponse;
import ar.edu.uade.deliverygo.logistica.service.RepartidorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// API-01 y API-02 (CU CORE) y los de evolucion: CU-08 (GET), CU-09 (vehiculos)
// y CU-10 (zona). Solo recibe el pedido, valida el request (@Valid) y delega en el service.
// Los errores los traduce GlobalExceptionHandler.
// Las anotaciones @Tag, @Operation, @ApiResponse y @Parameter solo documentan la API
// (Swagger UI y el contrato OpenAPI en /v3/api-docs.yaml); no cambian lo que hace el codigo
@RestController
@RequestMapping("/api/v1/couriers")
@Tag(name = "Repartidores", description = "Alta, disponibilidad, consulta, vehículos y zona de los repartidores")
public class RepartidorController {

    // RN-10: identidad del usuario que hace el pedido (simula el usuarioId del JWT)
    public static final String HEADER_USUARIO = "X-Usuario-Id";

    private final RepartidorService repartidorService;

    public RepartidorController(RepartidorService repartidorService) {
        this.repartidorService = repartidorService;
    }

    // API-01 / CU-01: registrar repartidor -> 201
    // POST /api/v1/couriers
    // { "usuarioId": 1, "nombre": "Juan", "apellido": "Perez", "dni": "30123456",
    //   "telefono": "1155554444", "vehiculo": { "tipo": "MOTO", "patente": "AB123CD" } }
    @Operation(summary = "CU-01 Registrar repartidor",
            description = "Da de alta un repartidor vinculado a un usuario existente, con su vehículo. "
                    + "Queda ACTIVO y NO_DISPONIBLE.")
    @ApiResponse(responseCode = "201", description = "Repartidor registrado")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: faltan datos o tienen formato inválido. "
            + "VEHICLE_PLATE_REQUIRED: MOTO o AUTO sin patente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "COURIER_DNI_DUPLICATED: el DNI ya está registrado. "
            + "COURIER_ALREADY_REGISTERED: el usuario ya es repartidor. "
            + "VEHICLE_PLATE_DUPLICATED: la patente ya está registrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "USER_NOT_ELIGIBLE: el usuario no existe, "
            + "está inactivo o no tiene rol REPARTIDOR",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<RepartidorResponse> registrar(
            @Valid @RequestBody RegistrarRepartidorRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(repartidorService.registrarRepartidor(request));
    }

    // API-02 / CU-02: cambiar disponibilidad -> 200
    // PATCH /api/v1/couriers/{id}/availability   (header X-Usuario-Id: 1)
    // { "disponibilidad": "DISPONIBLE" }
    @Operation(summary = "CU-02 Cambiar disponibilidad del repartidor",
            description = "El repartidor titular se pone DISPONIBLE o NO_DISPONIBLE. "
                    + "OCUPADO lo maneja solo el sistema. Pedir el mismo valor no es un error.")
    @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: falta la disponibilidad, "
            + "se pidió OCUPADO, el valor no existe o falta el header X-Usuario-Id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el usuario no es el titular del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "COURIER_HAS_ACTIVE_DELIVERY: tiene una entrega activa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "COURIER_INACTIVE: el repartidor está inactivo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/availability")
    public ResponseEntity<RepartidorResponse> cambiarDisponibilidad(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody CambiarDisponibilidadRequest request) {

        return ResponseEntity.ok(
                repartidorService.cambiarDisponibilidad(id, request, usuarioId));
    }

    // CU-08: datos del repartidor -> 200
    // GET /api/v1/couriers/{id}
    @Operation(summary = "CU-08 Consultar repartidor",
            description = "Devuelve los datos del repartidor con su vehículo activo y su zona.")
    @ApiResponse(responseCode = "200", description = "Datos del repartidor")
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<RepartidorResponse> buscarPorId(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(repartidorService.buscarPorId(id));
    }

    // CU-09: vehiculos del repartidor -> 200
    // GET /api/v1/couriers/{id}/vehicles
    @Operation(summary = "CU-09 Listar vehículos del repartidor",
            description = "Devuelve todos los vehículos del repartidor, indicando cuál es el activo.")
    @ApiResponse(responseCode = "200", description = "Vehículos del repartidor")
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}/vehicles")
    public ResponseEntity<List<VehiculoResponse>> listarVehiculos(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(repartidorService.listarVehiculos(id));
    }

    // CU-09: agregar vehiculo -> 201
    // POST /api/v1/couriers/{id}/vehicles   (header X-Usuario-Id: 1)
    // { "tipo": "AUTO", "patente": "AC456EF", "descripcion": "Fiat Uno" }
    @Operation(summary = "CU-09 Agregar vehículo",
            description = "El repartidor titular agrega un vehículo. Queda inactivo hasta que lo active. "
                    + "Devuelve todos sus vehículos.")
    @ApiResponse(responseCode = "201", description = "Vehículo agregado")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: faltan datos o falta el header X-Usuario-Id. "
            + "VEHICLE_PLATE_REQUIRED: MOTO o AUTO sin patente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el usuario no es el titular del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "VEHICLE_PLATE_DUPLICATED: la patente ya está registrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{id}/vehicles")
    public ResponseEntity<List<VehiculoResponse>> agregarVehiculo(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody AgregarVehiculoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(repartidorService.agregarVehiculo(id, request, usuarioId));
    }

    // CU-09: cambiar el vehiculo activo -> 200
    // PATCH /api/v1/couriers/{id}/vehicles/{vehiculoId}/activate   (header X-Usuario-Id: 1)
    @Operation(summary = "CU-09 Activar vehículo",
            description = "El repartidor titular elige con qué vehículo trabaja. Los demás quedan inactivos. "
                    + "No se puede cambiar durante una entrega.")
    @ApiResponse(responseCode = "200", description = "Vehículo activado")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: falta el header X-Usuario-Id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el usuario no es el titular del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe. "
            + "VEHICLE_NOT_FOUND: el vehículo no es del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "COURIER_HAS_ACTIVE_DELIVERY: tiene una entrega en curso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/vehicles/{vehiculoId}/activate")
    public ResponseEntity<List<VehiculoResponse>> activarVehiculo(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del vehículo", example = "2")
            @PathVariable Long vehiculoId,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId) {

        return ResponseEntity.ok(repartidorService.activarVehiculo(id, vehiculoId, usuarioId));
    }

    // CU-09: quitar vehiculo -> 204
    // DELETE /api/v1/couriers/{id}/vehicles/{vehiculoId}   (header X-Usuario-Id: 1)
    @Operation(summary = "CU-09 Quitar vehículo",
            description = "El repartidor titular da de baja un vehículo. El activo no se puede quitar.")
    @ApiResponse(responseCode = "204", description = "Vehículo quitado")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: falta el header X-Usuario-Id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el usuario no es el titular del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe. "
            + "VEHICLE_NOT_FOUND: el vehículo no es del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "VEHICLE_IS_ACTIVE: es el vehículo activo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}/vehicles/{vehiculoId}")
    public ResponseEntity<Void> quitarVehiculo(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del vehículo", example = "2")
            @PathVariable Long vehiculoId,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId) {

        repartidorService.quitarVehiculo(id, vehiculoId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    // CU-10: zona de operacion del repartidor -> 200
    // PUT /api/v1/couriers/{id}/zone   (header X-Usuario-Id: 1)
    // { "zonaId": 1 }
    @Operation(summary = "CU-10 Asignar zona al repartidor",
            description = "El repartidor titular elige la zona en la que trabaja. Las entregas con zona "
                    + "solo se asignan a repartidores de esa zona.")
    @ApiResponse(responseCode = "200", description = "Zona asignada")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: falta el zonaId o el header X-Usuario-Id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: el usuario no es el titular del repartidor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "COURIER_NOT_FOUND: el repartidor no existe. "
            + "ZONE_NOT_FOUND: la zona no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}/zone")
    public ResponseEntity<RepartidorResponse> asignarZona(
            @Parameter(description = "Id del repartidor", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del usuario que hace el pedido (simula el usuario del JWT)",
                    example = "1")
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody AsignarZonaRequest request) {

        return ResponseEntity.ok(repartidorService.asignarZona(id, request, usuarioId));
    }
}
