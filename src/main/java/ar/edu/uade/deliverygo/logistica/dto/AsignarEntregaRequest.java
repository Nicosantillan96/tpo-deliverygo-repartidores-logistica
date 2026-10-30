package ar.edu.uade.deliverygo.logistica.dto;

// DTO-04: entrada de POST /api/v1/deliveries/assign (CU-03)
// repartidorId es opcional: lo usa un administrador para elegir un
// repartidor puntual (DM-07). No identifica a quien hace la solicitud
public class AsignarEntregaRequest {

    private Long pedidoId;
    private Long clienteId;
    private Long comercioId;
    private String direccionRetiro;
    private String direccionEntrega;
    private Long repartidorId;

    public AsignarEntregaRequest() {
    }

    public AsignarEntregaRequest(Long pedidoId, Long clienteId, Long comercioId,
                                 String direccionRetiro, String direccionEntrega,
                                 Long repartidorId) {
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.comercioId = comercioId;
        this.direccionRetiro = direccionRetiro;
        this.direccionEntrega = direccionEntrega;
        this.repartidorId = repartidorId;
    }

    //GETTERS & SETTERS
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

    public Long getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(Long repartidorId) {
        this.repartidorId = repartidorId;
    }
}
