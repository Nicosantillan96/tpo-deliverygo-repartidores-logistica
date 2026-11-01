package ar.edu.uade.deliverygo.logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO-07: entrada de POST /api/v1/deliveries/{id}/complete (CU-05)
// No lleva repartidorId: la identidad sale de la autenticacion (RN-10)
public class ConfirmarEntregaRequest {

    // Que sea el codigo correcto lo valida la entidad (RN-13, 422 INVALID_CONFIRMATION_CODE).
    // Aca solo se exige que venga (400 VALIDATION_ERROR)
    @NotBlank(message = "El código de confirmación es obligatorio")
    private String codigoConfirmacion;

    // Opcional. El limite coincide con el largo de la columna del historial
    @Size(max = 255, message = "La observación puede tener hasta 255 caracteres")
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
