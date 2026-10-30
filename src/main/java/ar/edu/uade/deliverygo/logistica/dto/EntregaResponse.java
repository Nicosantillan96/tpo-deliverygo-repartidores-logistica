package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;

import java.time.LocalDateTime;

// DTO-05: salida de CU-03, CU-04 y CU-05. Solo lectura: no tiene setters
// No incluye el codigo de confirmacion: solo lo recibe el cliente (RN-13)
public class EntregaResponse {

    private final Long id;
    private final Long pedidoId;
    private final Long repartidorId;
    private final EstadoEntrega estado;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaAsignacion;
    private final LocalDateTime fechaRetiro;
    private final LocalDateTime fechaEntrega;

    public EntregaResponse(Long id, Long pedidoId, Long repartidorId, EstadoEntrega estado,
                           LocalDateTime fechaCreacion, LocalDateTime fechaAsignacion,
                           LocalDateTime fechaRetiro, LocalDateTime fechaEntrega) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.repartidorId = repartidorId;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaRetiro = fechaRetiro;
        this.fechaEntrega = fechaEntrega;
    }

    //GETTERS
    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Long getRepartidorId() {
        return repartidorId;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public LocalDateTime getFechaRetiro() {
        return fechaRetiro;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }
}
