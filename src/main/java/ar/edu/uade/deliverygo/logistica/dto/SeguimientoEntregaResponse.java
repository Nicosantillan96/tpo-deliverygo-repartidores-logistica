package ar.edu.uade.deliverygo.logistica.dto;

import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;

import java.time.LocalDateTime;
import java.util.List;

// DTO-12: salida de GET /api/v1/deliveries/{id} (CU-08). Solo lectura: no tiene setters
// Los datos de EntregaResponse mas la zona y el historial completo.
// Tampoco incluye el codigo de confirmacion (RN-13)
public class SeguimientoEntregaResponse {

    private final Long id;
    private final Long pedidoId;
    private final Long repartidorId;
    private final Long zonaId;
    private final EstadoEntrega estado;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaAsignacion;
    private final LocalDateTime fechaRetiro;
    private final LocalDateTime fechaEntrega;
    private final List<HistorialEstadoResponse> historial;

    public SeguimientoEntregaResponse(Long id, Long pedidoId, Long repartidorId, Long zonaId,
                                      EstadoEntrega estado, LocalDateTime fechaCreacion,
                                      LocalDateTime fechaAsignacion, LocalDateTime fechaRetiro,
                                      LocalDateTime fechaEntrega,
                                      List<HistorialEstadoResponse> historial) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.repartidorId = repartidorId;
        this.zonaId = zonaId;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaRetiro = fechaRetiro;
        this.fechaEntrega = fechaEntrega;
        this.historial = historial;
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

    public Long getZonaId() {
        return zonaId;
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

    public List<HistorialEstadoResponse> getHistorial() {
        return historial;
    }
}
