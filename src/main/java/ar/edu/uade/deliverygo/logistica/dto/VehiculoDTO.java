package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import jakarta.validation.constraints.NotNull;

// DTO-09: se usa anidado en RegistrarRepartidorRequest y RepartidorResponse
// Las anotaciones solo se aplican en la entrada (RegistrarRepartidorRequest con @Valid)
public class VehiculoDTO {

    @NotNull(message = "El tipo de vehículo es obligatorio")
    private TipoVehiculo tipo;

    // Obligatoria solo para MOTO y AUTO (RN-04). Como depende del tipo,
    // no alcanza con una anotacion: la valida el service
    private String patente;
    private String descripcion;

    public VehiculoDTO() {
    }

    public VehiculoDTO(TipoVehiculo tipo, String patente, String descripcion) {
        this.tipo = tipo;
        this.patente = patente;
        this.descripcion = descripcion;
    }

    //GETTERS & SETTERS
    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) {
        this.patente = patente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
