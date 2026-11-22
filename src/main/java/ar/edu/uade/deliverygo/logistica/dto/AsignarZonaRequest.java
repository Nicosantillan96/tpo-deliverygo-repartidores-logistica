package ar.edu.uade.deliverygo.logistica.dto;

import jakarta.validation.constraints.NotNull;

// DTO-15: entrada de PUT /api/v1/couriers/{id}/zone (CU-10)
// No lleva repartidorId ni usuarioId: el titular sale de la autenticacion (RN-10)
public class AsignarZonaRequest {

    @NotNull(message = "El zonaId es obligatorio")
    private Long zonaId;

    public AsignarZonaRequest() {
    }

    public AsignarZonaRequest(Long zonaId) {
        this.zonaId = zonaId;
    }

    //GETTERS & SETTERS
    public Long getZonaId() {
        return zonaId;
    }

    public void setZonaId(Long zonaId) {
        this.zonaId = zonaId;
    }
}
