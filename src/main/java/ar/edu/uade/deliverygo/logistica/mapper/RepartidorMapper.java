package ar.edu.uade.deliverygo.logistica.mapper;

import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoDTO;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import org.springframework.stereotype.Component;

import java.util.Objects;

// Convierte la entidad Repartidor.
// La entidad nunca se expone directamente en la API: el service la convierte aca
// antes de devolverla. Solo transforma datos, no aplica reglas de negocio
@Component
public class RepartidorMapper {

    public RepartidorResponse toResponse(Repartidor repartidor) {
        return new RepartidorResponse(
                repartidor.getId(),
                repartidor.getUsuarioId(),
                repartidor.getNombre(),
                repartidor.getApellido(),
                repartidor.getDni(),
                repartidor.getTelefono(),
                repartidor.getEstado(),
                repartidor.getDisponibilidad(),
                toVehiculoDTO(repartidor.vehiculoActivo()),
                repartidor.getFechaAlta());
    }

    // DTO-09: se devuelve el vehiculo activo (REL-01); null si no tiene
    private VehiculoDTO toVehiculoDTO(Vehiculo vehiculo) {
        if (Objects.isNull(vehiculo)) {
            return null;
        }
        return new VehiculoDTO(vehiculo.getTipo(), vehiculo.getPatente(), vehiculo.getDescripcion());
    }
}
