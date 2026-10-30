package ar.edu.uade.deliverygo.logistica.model;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
@Entity
@Table(name = "entregas")

public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //identificador del pedido del modulo Pedidos (sin relacion JPA)
    @Column(name = "pedido_id",nullable = false)
    private Long pedidoId;
    @Column(name = "cliente_id",nullable = false)
    private Long clienteId;
    @Column(name = "comercio_id",nullable = false)
    private Long comercioId;
    @Column(nullable = false)
    private String direccionRetiro;
    @Column(nullable = false)
    private String direccionEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrega estado;
    //se genera al asignar la entrega; no se expone en la API
    @Column(name = "codigo_confirmacion", length = 4)
    private String codigoConfirmacion;
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaRetiro;
    private LocalDateTime fechaEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private Repartidor repartidor;

    //el historial se guarda en cascada con la entrega y nunca se borra
    @OneToMany(
            mappedBy = "entrega",
            cascade = CascadeType.ALL
    )
    private List<HistorialEstadoEntrega> historial = new ArrayList<>();


   public Entrega() {}

    // getters y setters
    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getComercioId() {
        return comercioId;
    }

    public void setComercioId(Long comercioId) {
        this.comercioId = comercioId;
    }

    public String getDireccionRetiro() {
        return direccionRetiro;
    }

    public void setDireccionRetiro(String direccionRetiro) {
        this.direccionRetiro = direccionRetiro;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }

    public void setEstado(EstadoEntrega estado) {
        this.estado = estado;
    }

    public String getCodigoConfirmacion() {
        return codigoConfirmacion;
    }

    public void setCodigoConfirmacion(String codigoConfirmacion) {
        this.codigoConfirmacion = codigoConfirmacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public LocalDateTime getFechaRetiro() {
        return fechaRetiro;
    }

    public void setFechaRetiro(LocalDateTime fechaRetiro) {
        this.fechaRetiro = fechaRetiro;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Repartidor getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(Repartidor repartidor) {
        this.repartidor = repartidor;
    }

    public List<HistorialEstadoEntrega> getHistorial() {
        return historial;
    }

    public void agregarHistorial(HistorialEstadoEntrega registro) {
        historial.add(registro);
        registro.setEntrega(this);
    }

    // Metodos de dominio

    // CU-03: PENDIENTE_ASIGNACION -> ASIGNADA (RN-07, RN-08, RN-13)
    // El codigo lo genera el service (SRV-03); la entidad garantiza que no falte
    public void asignar(Repartidor repartidor, String codigoConfirmacion) {
        validarTransicion(EstadoEntrega.ASIGNADA);
        if (Objects.isNull(codigoConfirmacion) || !codigoConfirmacion.matches("\\d{4}")) {
            throw new IllegalArgumentException("El código de confirmación debe tener 4 dígitos");
        }
        repartidor.ocupar();
        this.repartidor = repartidor;
        this.codigoConfirmacion = codigoConfirmacion;
        this.fechaAsignacion = LocalDateTime.now();
        this.estado = EstadoEntrega.ASIGNADA;
    }

    // CU-04: ASIGNADA -> RETIRADA (RN-08)
    public void registrarRetiro() {
        validarTransicion(EstadoEntrega.RETIRADA);
        this.fechaRetiro = LocalDateTime.now();
        this.estado = EstadoEntrega.RETIRADA;
    }

    // CU-05: RETIRADA / EN_CAMINO -> COMPLETADA (RN-07, RN-08, RN-09, RN-13)
    public void completar(String codigo) {
        // RN-09: no se puede completar una entrega que no fue retirada
        if (estado == EstadoEntrega.ASIGNADA) {
            throw new ReglaNegocioException(CodigoError.DELIVERY_NOT_PICKED_UP,
                    "La entrega todavía no fue retirada");
        }
        validarTransicion(EstadoEntrega.COMPLETADA);
        // RN-13: si el codigo es incorrecto la entrega no cambia de estado
        if (!Objects.equals(codigoConfirmacion, codigo)) {
            throw new ReglaNegocioException(CodigoError.INVALID_CONFIRMATION_CODE,
                    "El código de confirmación es incorrecto");
        }
        this.fechaEntrega = LocalDateTime.now();
        this.estado = EstadoEntrega.COMPLETADA;
        repartidor.liberar();
    }

    // RN-08: maquina de estados de la entrega (seccion 7 de la especificacion)
    // Solo incluye las transiciones de los CU CORE; las de CU-06/CU-07 y la
    // cancelacion se agregan cuando se implementen esos casos de uso
    private void validarTransicion(EstadoEntrega destino) {
        boolean valida = switch (estado) {
            case PENDIENTE_ASIGNACION -> destino == EstadoEntrega.ASIGNADA;
            case ASIGNADA -> destino == EstadoEntrega.RETIRADA;
            case RETIRADA, EN_CAMINO -> destino == EstadoEntrega.COMPLETADA;
            case COMPLETADA, CANCELADA -> false;
        };
        if (!valida) {
            throw new ReglaNegocioException(CodigoError.DELIVERY_INVALID_STATE,
                    "No se puede pasar de " + estado + " a " + destino);
        }
    }
}
