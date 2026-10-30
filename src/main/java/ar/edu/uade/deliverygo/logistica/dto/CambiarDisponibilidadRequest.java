package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;

// DTO-03: entrada de PATCH /api/v1/couriers/{id}/availability (CU-02)
// Solo admite DISPONIBLE o NO_DISPONIBLE; SI ESTA OCUPADO lo rechaza el dominio (RN-07)
public class CambiarDisponibilidadRequest {

    private Disponibilidad disponibilidad;

    public CambiarDisponibilidadRequest() {
    }

    public CambiarDisponibilidadRequest(Disponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    //GETTERS & SETTERS
    public Disponibilidad getDisponibilidad() {
        return disponibilidad;
    }

    public void setDisponibilidad(Disponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }
}
