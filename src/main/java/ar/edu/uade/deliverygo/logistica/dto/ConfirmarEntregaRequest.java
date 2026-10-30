package ar.edu.uade.deliverygo.logistica.dto;

// DTO-07: entrada de POST /api/v1/deliveries/{id}/complete (CU-05)
// No lleva repartidorId: la identidad sale de la autenticacion (RN-10)
public class ConfirmarEntregaRequest {

    private String codigoConfirmacion;
    private String observacion;

    public ConfirmarEntregaRequest() {
    }

    public ConfirmarEntregaRequest(String codigoConfirmacion, String observacion) {
        this.codigoConfirmacion = codigoConfirmacion;
        this.observacion = observacion;
    }

    //GETTERS & SETTERS
    public String getCodigoConfirmacion() {
        return codigoConfirmacion;
    }

    public void setCodigoConfirmacion(String codigoConfirmacion) {
        this.codigoConfirmacion = codigoConfirmacion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
