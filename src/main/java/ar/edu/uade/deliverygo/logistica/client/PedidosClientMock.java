package ar.edu.uade.deliverygo.logistica.client;

import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

// Mock declarado de INT-03 e INT-04 mientras el modulo Pedidos no este disponible.
// Simula los pedidos con una lista fija:
// - 100 a 105: CONFIRMADO -> se puede crear la entrega
// - 106: PENDIENTE -> ORDER_NOT_CONFIRMED
// - cualquier otro: no existe -> ORDER_NOT_FOUND
@Component
public class PedidosClientMock implements PedidosClient {

    private static final Logger log = LoggerFactory.getLogger(PedidosClientMock.class);

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

    // INT-04: no envia nada, solo deja el aviso en el log
    @Override
    public void informarEstadoEntrega(Long pedidoId, EstadoEntrega estado) {
        log.info("[MOCK Pedidos] La entrega del pedido {} paso a {}", pedidoId, estado);
    }
}
