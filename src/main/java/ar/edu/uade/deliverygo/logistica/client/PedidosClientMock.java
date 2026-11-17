package ar.edu.uade.deliverygo.logistica.client;

import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

// Mock declarado de INT-03 e INT-04 mientras el modulo Pedidos no este disponible.
// El modulo Pedidos (Grupo 5) identifica los pedidos con UUID, por eso pedidoId es UUID.
// Simula los pedidos con una lista fija. Para que se lean facil, el numero va al final del UUID
// (ver pedido(int)): el pedido 100 es 00000000-0000-0000-0000-000000000100
// - 100 a 105: CONFIRMADO -> se puede crear la entrega
// - 106: PENDIENTE -> ORDER_NOT_CONFIRMED
// - cualquier otro: no existe -> ORDER_NOT_FOUND
@Component
public class PedidosClientMock implements PedidosClient {

    private static final Logger log = LoggerFactory.getLogger(PedidosClientMock.class);

    private static final Map<UUID, PedidoExterno> PEDIDOS = Map.of(
            pedido(100), new PedidoExterno(pedido(100), "CONFIRMADO"),
            pedido(101), new PedidoExterno(pedido(101), "CONFIRMADO"),
            pedido(102), new PedidoExterno(pedido(102), "CONFIRMADO"),
            pedido(103), new PedidoExterno(pedido(103), "CONFIRMADO"),
            pedido(104), new PedidoExterno(pedido(104), "CONFIRMADO"),
            pedido(105), new PedidoExterno(pedido(105), "CONFIRMADO"),
            pedido(106), new PedidoExterno(pedido(106), "PENDIENTE")
    );

    // UUID del pedido de prueba con ese numero: pedido(100) -> 00000000-0000-0000-0000-000000000100
    public static UUID pedido(int numero) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(numero));
    }

    @Override
    public PedidoExterno buscarPedido(UUID pedidoId) {
        return PEDIDOS.get(pedidoId);
    }

    // INT-04: no envia nada, solo deja el aviso en el log
    @Override
    public void informarEstadoEntrega(UUID pedidoId, EstadoEntrega estado) {
        log.info("[MOCK Pedidos] La entrega del pedido {} paso a {}", pedidoId, estado);
    }
}
