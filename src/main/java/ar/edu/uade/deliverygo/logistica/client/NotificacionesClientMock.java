package ar.edu.uade.deliverygo.logistica.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Mock declarado de INT-05 mientras el modulo Notificaciones no este disponible.
// No envia nada: solo deja el aviso en el log de la aplicacion
@Component
public class NotificacionesClientMock implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientMock.class);

    @Override
    public void avisarAsignacion(Long entregaId, Long repartidorId, Long clienteId,
                                 String codigoConfirmacion) {
        log.info("[MOCK Notificaciones] Entrega {} asignada al repartidor {}. "
                        + "Codigo de confirmacion enviado al cliente {}",
                entregaId, repartidorId, clienteId);
    }

    @Override
    public void avisarRetiro(Long entregaId, Long clienteId) {
        log.info("[MOCK Notificaciones] Entrega {} retirada. Aviso enviado al cliente {}",
                entregaId, clienteId);
    }
}
