package ar.edu.uade.deliverygo.logistica.dto;

import jakarta.validation.constraints.Size;

// DTO-06: entrada de POST /api/v1/deliveries/{id}/pickup (CU-04)
// No lleva repartidorId: la identidad sale de la autenticacion (RN-10)
public class RegistrarRetiroRequest {

    // Opcional. El limite coincide con el largo de la columna del historial
    @Size(max = 255, message = "La observación puede tener hasta 255 caracteres")
    private String observacion;

    public RegistrarRetiroRequest() {
    }

    public RegistrarRetiroRequest(String observacion) {
        this.observacion = observacion;
    }

    //GETTERS & SETTERS
    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
