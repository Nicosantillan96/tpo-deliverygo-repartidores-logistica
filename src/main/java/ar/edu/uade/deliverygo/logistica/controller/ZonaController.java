package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.CrearZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.ErrorResponse;
import ar.edu.uade.deliverygo.logistica.dto.ZonaResponse;
import ar.edu.uade.deliverygo.logistica.service.ZonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// CU-10: alta y listado de zonas de operacion. Solo recibe el pedido, valida el
// request (@Valid) y delega en el service. Como el alta de repartidores (CU-01),
// es una operacion administrativa: no usa el header X-Usuario-Id (los roles no se controlan)
@RestController
@RequestMapping("/api/v1/zones")
@Tag(name = "Zonas", description = "Zonas de operación de los repartidores")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    // POST /api/v1/zones -> 201
    // { "nombre": "Palermo" }
    @Operation(summary = "CU-10 Crear zona", description = "Da de alta una zona de operación.")
    @ApiResponse(responseCode = "201", description = "Zona creada")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: falta el nombre o es demasiado largo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "ZONE_NAME_DUPLICATED: ya existe una zona con ese nombre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<ZonaResponse> crear(@Valid @RequestBody CrearZonaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(zonaService.crearZona(request));
    }

    // GET /api/v1/zones -> 200
    @Operation(summary = "CU-10 Listar zonas", description = "Devuelve las zonas ordenadas por nombre.")
    @ApiResponse(responseCode = "200", description = "Zonas")
    @GetMapping
    public ResponseEntity<List<ZonaResponse>> listar() {
        return ResponseEntity.ok(zonaService.listarZonas());
    }
}
