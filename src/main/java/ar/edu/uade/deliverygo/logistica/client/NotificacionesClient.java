package ar.edu.uade.deliverygo.logistica.client;

// INT-05: Logistica -> Notificaciones (POST /api/v1/notifications)
// Hoy la implementa NotificacionesClientMock. Los avisos no son bloqueantes:
// si fallan, la operacion no se revierte (DM-12).
public interface NotificacionesClient {

    // CU-03: avisa al repartidor que tiene una entrega y le manda al cliente
    // el codigo de confirmacion (RN-13)
    void avisarAsignacion(Long entregaId, Long repartidorId, Long clienteId,
                          String codigoConfirmacion);

    // CU-04: avisa al cliente que el repartidor retiro el pedido
    void avisarRetiro(Long entregaId, Long clienteId);
}
