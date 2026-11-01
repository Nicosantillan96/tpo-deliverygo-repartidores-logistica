package ar.edu.uade.deliverygo.logistica.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Mock declarado de INT-06 mientras el modulo Analitica no este disponible.
// No envia nada: solo deja el evento en el log de la aplicacion
@Component
public class AnaliticaClientMock implements AnaliticaClient {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaClientMock.class);

    @Override
    public void registrarEntregaCompletada(Long entregaId, Long repartidorId,
                                           LocalDateTime fechaCreacion,
                                           LocalDateTime fechaAsignacion,
                                           LocalDateTime fechaRetiro,
                                           LocalDateTime fechaEntrega) {
        log.info("[MOCK Analitica] DELIVERY_COMPLETED entrega {} repartidor {} "
                        + "(creada {}, asignada {}, retirada {}, entregada {})",
                entregaId, repartidorId, fechaCreacion, fechaAsignacion, fechaRetiro, fechaEntrega);
    }
}
