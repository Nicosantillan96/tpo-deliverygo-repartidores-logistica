package ar.edu.uade.deliverygo.logistica.mapper;

import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.HistorialEstadoResponse;
import ar.edu.uade.deliverygo.logistica.dto.SeguimientoEntregaResponse;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

// Convierte la entidad Entrega.
// Nunca copia el codigo de confirmacion: solo lo recibe el cliente.
// Solo transforma datos, no aplica reglas de negocio
@Component
public class EntregaMapper {

    public EntregaResponse toResponse(Entrega entrega) {
        return new EntregaResponse(
                entrega.getId(),
                entrega.getPedidoId(),
                repartidorId(entrega),
                entrega.getEstado(),
                entrega.getFechaCreacion(),
                entrega.getFechaAsignacion(),
                entrega.getFechaRetiro(),
                entrega.getFechaEntrega());
    }

    // CU-08: la entrega con su historial, en el orden en que se registraron los cambios
    public SeguimientoEntregaResponse toSeguimientoResponse(Entrega entrega) {
        List<HistorialEstadoResponse> historial = entrega.getHistorial().stream()
                .map(h -> new HistorialEstadoResponse(h.getEstadoAnterior(), h.getEstadoNuevo(),
                        h.getFechaHora(), h.getUsuarioActorId(), h.getObservacion()))
                .toList();
        return new SeguimientoEntregaResponse(
                entrega.getId(),
                entrega.getPedidoId(),
                repartidorId(entrega),
                entrega.getZonaId(),
                entrega.getEstado(),
                entrega.getFechaCreacion(),
                entrega.getFechaAsignacion(),
                entrega.getFechaRetiro(),
                entrega.getFechaEntrega(),
                historial);
    }

    private Long repartidorId(Entrega entrega) {
        return Objects.isNull(entrega.getRepartidor()) ? null : entrega.getRepartidor().getId();
    }
}
