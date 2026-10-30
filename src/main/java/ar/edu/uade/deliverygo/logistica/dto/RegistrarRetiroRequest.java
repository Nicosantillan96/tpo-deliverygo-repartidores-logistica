package ar.edu.uade.deliverygo.logistica.dto;

// DTO-06: entrada de POST /api/v1/deliveries/{id}/pickup (CU-04)
// No lleva repartidorId: la identidad sale de la autenticacion (RN-10)
public class RegistrarRetiroRequest {

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
