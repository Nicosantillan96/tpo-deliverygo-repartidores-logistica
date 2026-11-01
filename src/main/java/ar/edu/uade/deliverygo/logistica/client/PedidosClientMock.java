package ar.edu.uade.deliverygo.logistica.client;

import org.springframework.stereotype.Component;

import java.util.Map;

// Mock declarado de INT-03 mientras el modulo Pedidos no este disponible.
// Simula los pedidos con una lista fija:
// - 100 a 105: CONFIRMADO -> se puede crear la entrega
// - 106: PENDIENTE -> ORDER_NOT_CONFIRMED
// - cualquier otro: no existe -> ORDER_NOT_FOUND
@Component
public class PedidosClientMock implements PedidosClient {

    private static final Map<Long, PedidoExterno> PEDIDOS = Map.of(
            100L, new PedidoExterno(100L, "CONFIRMADO"),
            101L, new PedidoExterno(101L, "CONFIRMADO"),
            102L, new PedidoExterno(102L, "CONFIRMADO"),
            103L, new PedidoExterno(103L, "CONFIRMADO"),
            104L, new PedidoExterno(104L, "CONFIRMADO"),
            105L, new PedidoExterno(105L, "CONFIRMADO"),
            106L, new PedidoExterno(106L, "PENDIENTE")
    );

    @Override
    public PedidoExterno buscarPedido(Long pedidoId) {
        return PEDIDOS.get(pedidoId);
    }
}
