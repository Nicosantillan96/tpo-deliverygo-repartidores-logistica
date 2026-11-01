package ar.edu.uade.deliverygo.logistica.client;

// INT-03: Logistica -> Pedidos (GET /api/v1/orders/{id})
// El service depende de esta interfaz. Hoy la implementa PedidosClientMock;
// cuando exista el modulo Pedidos se agrega la implementacion HTTP real.
public interface PedidosClient {

    // Devuelve el pedido, o null si no existe en el modulo Pedidos
    PedidoExterno buscarPedido(Long pedidoId);
}
