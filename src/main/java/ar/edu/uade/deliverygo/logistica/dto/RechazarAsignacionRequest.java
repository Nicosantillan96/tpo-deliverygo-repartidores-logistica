package ar.edu.uade.deliverygo.logistica.dto;

import jakarta.validation.constraints.Size;

// DTO-10: entrada de POST /api/v1/deliveries/{id}/reject (CU-06)
// El motivo es opcional y queda en el historial. No lleva repartidorId:
// la identidad sale de la autenticacion (RN-10)
public class RechazarAsignacionRequest {

    @Size(max = 255, message = "El motivo puede tener hasta 255 caracteres")
    private String motivo;

    public RechazarAsignacionRequest() {
    }

    public RechazarAsignacionRequest(String motivo) {
        this.motivo = motivo;
    }

    //GETTERS & SETTERS
    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
