package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO-13: entrada de POST /api/v1/couriers/{id}/vehicles (CU-09)
// Mismos datos que VehiculoDTO; es un Request porque todo lo que entra a la API
// es un ...Request validado con @Valid. El vehiculo nuevo no queda activo:
// se activa despues con PATCH .../activate
public class AgregarVehiculoRequest {

    @NotNull(message = "El tipo de vehículo es obligatorio")
    private TipoVehiculo tipo;

    // Obligatoria solo para MOTO y AUTO (RN-04): depende del tipo, la valida el service
    @Size(max = 20, message = "La patente puede tener hasta 20 caracteres")
    private String patente;

    @Size(max = 255, message = "La descripción puede tener hasta 255 caracteres")
    private String descripcion;

    public AgregarVehiculoRequest() {
    }

    public AgregarVehiculoRequest(TipoVehiculo tipo, String patente, String descripcion) {
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
