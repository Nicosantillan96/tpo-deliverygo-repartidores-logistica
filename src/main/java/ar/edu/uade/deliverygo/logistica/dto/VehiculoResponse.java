package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;

// DTO-14: un vehiculo del repartidor, salida de CU-09. Solo lectura: no tiene setters
// A diferencia de VehiculoDTO, tiene el id (para activarlo o quitarlo) y si esta activo
public class VehiculoResponse {

    private final Long id;
    private final TipoVehiculo tipo;
    private final String patente;
    private final String descripcion;
    private final boolean activo;

    public VehiculoResponse(Long id, TipoVehiculo tipo, String patente, String descripcion,
                            boolean activo) {
        this.id = id;
        this.tipo = tipo;
        this.patente = patente;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    //GETTERS
    public Long getId() {
        return id;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public String getPatente() {
        return patente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }
}
