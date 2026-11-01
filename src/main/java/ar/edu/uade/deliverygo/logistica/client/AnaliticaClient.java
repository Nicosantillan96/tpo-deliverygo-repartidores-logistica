package ar.edu.uade.deliverygo.logistica.client;

import java.time.LocalDateTime;

// INT-06: Logistica -> Analitica (POST /api/v1/analytics/events)
// Hoy la implementa AnaliticaClientMock. No es bloqueante: si falla,
// la entrega completada no se revierte (DM-12).
public interface AnaliticaClient {

    // CU-05: evento DELIVERY_COMPLETED con los tiempos de la entrega,
    // para que Analitica calcule metricas (por ejemplo, demora entre asignacion y entrega)
    void registrarEntregaCompletada(Long entregaId, Long repartidorId,
                                    LocalDateTime fechaCreacion, LocalDateTime fechaAsignacion,
                                    LocalDateTime fechaRetiro, LocalDateTime fechaEntrega);
}
