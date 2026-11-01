package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.ConfirmarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRetiroRequest;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.service.EntregaService;
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
// en el service. Los errores los traduce GlobalExceptionHandler
@RestController
@RequestMapping("/api/v1/deliveries")
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
    @PostMapping("/{id}/pickup")
    public ResponseEntity<EntregaResponse> registrarRetiro(
            @PathVariable Long id,
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody(required = false) RegistrarRetiroRequest request) {

        RegistrarRetiroRequest retiro = Objects.isNull(request) ? new RegistrarRetiroRequest() : request;
        return ResponseEntity.ok(entregaService.registrarRetiro(id, retiro, usuarioId));
    }

    // API-05 / CU-05: confirmar entrega -> 200
    // POST /api/v1/deliveries/{id}/complete   (header X-Usuario-Id: 1)
    // { "codigoConfirmacion": "4821" }
    @PostMapping("/{id}/complete")
    public ResponseEntity<EntregaResponse> confirmarEntrega(
            @PathVariable Long id,
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody ConfirmarEntregaRequest request) {

        return ResponseEntity.ok(entregaService.confirmarEntrega(id, request, usuarioId));
    }
}
