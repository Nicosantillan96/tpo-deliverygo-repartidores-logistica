package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;

import java.time.LocalDateTime;

// DTO-02: salida de CU-01 y CU-02. Solo lectura: no tiene setters
// Incluye dni y telefono (decision del 01/11/2026; la especificacion no los incluia)
public class RepartidorResponse {

    private final Long id;
    private final Long usuarioId;
    private final String nombre;
    private final String apellido;
    private final String dni;
    private final String telefono;
    private final EstadoRepartidor estado;
    private final Disponibilidad disponibilidad;
    private final VehiculoDTO vehiculo;
    private final LocalDateTime fechaAlta;

    public RepartidorResponse(Long id, Long usuarioId, String nombre, String apellido,
                              String dni, String telefono,
                              EstadoRepartidor estado, Disponibilidad disponibilidad,
                              VehiculoDTO vehiculo, LocalDateTime fechaAlta) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.telefono = telefono;
        this.estado = estado;
        this.disponibilidad = disponibilidad;
        this.vehiculo = vehiculo;
        this.fechaAlta = fechaAlta;
    }

    //GETTERS
    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDni() {
        return dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public EstadoRepartidor getEstado() {
        return estado;
    }

    public Disponibilidad getDisponibilidad() {
        return disponibilidad;
    }

    public VehiculoDTO getVehiculo() {
        return vehiculo;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }
}
