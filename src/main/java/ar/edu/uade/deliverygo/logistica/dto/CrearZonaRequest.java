package ar.edu.uade.deliverygo.logistica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO-16: entrada de POST /api/v1/zones (CU-10)
// No tiene id: lo define el sistema
public class CrearZonaRequest {

    @NotBlank(message = "El nombre de la zona es obligatorio")
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres")
    private String nombre;

    public CrearZonaRequest() {
    }

    public CrearZonaRequest(String nombre) {
        this.nombre = nombre;
    }

    //GETTERS & SETTERS
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
