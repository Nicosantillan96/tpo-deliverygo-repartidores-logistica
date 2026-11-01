package ar.edu.uade.deliverygo.logistica.client;

// Datos que Logistica necesita del modulo Pedidos para RN-11 (INT-03).
// No es una entidad: el pedido pertenece a otro modulo y no se guarda aca (DM-01).
// Solo se incluyen los campos que usa CU-03; el contrato completo lo define Pedidos.
public class PedidoExterno {

    private final Long id;
    private final String estado;

    public PedidoExterno(Long id, String estado) {
        this.id = id;
        this.estado = estado;
    }

    //GETTERS
    public Long getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }
}
