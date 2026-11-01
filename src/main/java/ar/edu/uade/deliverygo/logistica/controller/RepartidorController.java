package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.service.RepartidorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// API-01 y API-02. Solo recibe el pedido, valida el request (@Valid) y delega en el service.
// Los errores los traduce GlobalExceptionHandler
@RestController
@RequestMapping("/api/v1/couriers")
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
    @PatchMapping("/{id}/availability")
    public ResponseEntity<RepartidorResponse> cambiarDisponibilidad(
            @PathVariable Long id,
            @RequestHeader(HEADER_USUARIO) Long usuarioId,
            @Valid @RequestBody CambiarDisponibilidadRequest request) {

        return ResponseEntity.ok(
                repartidorService.cambiarDisponibilidad(id, request, usuarioId));
    }
}
