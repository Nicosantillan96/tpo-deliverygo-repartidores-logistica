package ar.edu.uade.deliverygo.logistica.client;

import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;

// Integraciones con el modulo Pedidos. El service depende de esta interfaz.
// Hoy la implementa PedidosClientMock; cuando exista el modulo Pedidos se agrega
// la implementacion HTTP real.
public interface PedidosClient {

    // INT-03 (GET /api/v1/orders/{id}): devuelve el pedido, o null si no existe
    PedidoExterno buscarPedido(Long pedidoId);

    // INT-04: informa a Pedidos el nuevo estado de la entrega (CU-04, CU-05).
    // El endpoint esta a acordar con el grupo de Pedidos (seccion 16 de la especificacion).
    // No es bloqueante: si falla, la operacion no se revierte (DM-12)
    void informarEstadoEntrega(Long pedidoId, EstadoEntrega estado);
}
