package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.StatusResponse;
import ar.edu.uade.deliverygo.logistica.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /status: los demas equipos lo usan para confirmar que Logistica esta levantada.
// 200 si el modulo y su base funcionan; 503 si la base no responde
@RestController
@Tag(name = "Estado", description = "Estado del módulo para la integración")
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @Operation(summary = "Estado del módulo",
            description = "Confirma que el módulo Logística está levantado y que su base de datos responde.")
    @ApiResponse(responseCode = "200", description = "El módulo funciona (status UP)")
    @ApiResponse(responseCode = "503", description = "La base de datos no responde (status DOWN)")
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        StatusResponse estado = statusService.consultarEstado();
        HttpStatus codigo = StatusResponse.UP.equals(estado.getStatus())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(codigo).body(estado);
    }
}
