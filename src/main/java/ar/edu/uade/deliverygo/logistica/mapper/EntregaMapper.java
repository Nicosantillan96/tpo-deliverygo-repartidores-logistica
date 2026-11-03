package ar.edu.uade.deliverygo.logistica.mapper;

import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import org.springframework.stereotype.Component;

import java.util.Objects;

// Convierte la entidad Entrega.
// Nunca copia el codigo de confirmacion: solo lo recibe el cliente.
// Solo transforma datos, no aplica reglas de negocio
@Component
public class EntregaMapper {

    public EntregaResponse toResponse(Entrega entrega) {
        Long repartidorId = Objects.isNull(entrega.getRepartidor()) ? null
                : entrega.getRepartidor().getId();
        return new EntregaResponse(
                entrega.getId(),
                entrega.getPedidoId(),
                repartidorId,
                entrega.getEstado(),
                entrega.getFechaCreacion(),
                entrega.getFechaAsignacion(),
                entrega.getFechaRetiro(),
                entrega.getFechaEntrega());
    }
}
