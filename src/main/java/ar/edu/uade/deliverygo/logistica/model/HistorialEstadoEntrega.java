package ar.edu.uade.deliverygo.logistica.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estados_entrega")
public class HistorialEstadoEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //null en el primer registro (creacion de la entrega)
    @Enumerated(EnumType.STRING)
    private EstadoEntrega estadoAnterior;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrega estadoNuevo;
    @Column(name = "fecha_hora",nullable = false)
    private LocalDateTime fechaHora;
    @Column(nullable = false)
    private Long usuarioActorId;
    private String observacion;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "entrega_id",nullable = false)
    private Entrega entrega;

    public HistorialEstadoEntrega() {}

    //getters y setters

    public Long getId() {
        return id;
    }

    public EstadoEntrega getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoEntrega estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoEntrega getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoEntrega estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Long getUsuarioActorId() {
        return usuarioActorId;
    }

    public void setUsuarioActorId(Long usuarioActorId) {
        this.usuarioActorId = usuarioActorId;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Entrega getEntrega() {
        return entrega;
    }

    public void setEntrega(Entrega entrega) {
        this.entrega = entrega;
    }
}
