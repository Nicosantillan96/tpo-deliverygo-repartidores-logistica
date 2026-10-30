package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;

// DTO-09: se usa anidado en RegistrarRepartidorRequest y RepartidorResponse
public class VehiculoDTO {

    private TipoVehiculo tipo;
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
