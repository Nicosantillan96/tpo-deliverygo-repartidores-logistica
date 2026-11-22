package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;

import java.time.LocalDateTime;

// DTO-11: un cambio de estado de la entrega (RN-12), dentro de SeguimientoEntregaResponse (CU-08)
// estadoAnterior es null en el primer registro (creacion de la entrega).
// usuarioActorId 0 significa que el cambio lo hizo el sistema
public class HistorialEstadoResponse {

    private final EstadoEntrega estadoAnterior;
    private final EstadoEntrega estadoNuevo;
    private final LocalDateTime fechaHora;
    private final Long usuarioActorId;
    private final String observacion;

    public HistorialEstadoResponse(EstadoEntrega estadoAnterior, EstadoEntrega estadoNuevo,
                                   LocalDateTime fechaHora, Long usuarioActorId,
                                   String observacion) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHora = fechaHora;
        this.usuarioActorId = usuarioActorId;
        this.observacion = observacion;
    }

    //GETTERS
    public EstadoEntrega getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoEntrega getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public Long getUsuarioActorId() {
        return usuarioActorId;
    }

    public String getObservacion() {
        return observacion;
    }
}
