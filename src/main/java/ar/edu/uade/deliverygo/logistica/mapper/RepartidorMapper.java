package ar.edu.uade.deliverygo.logistica.mapper;

import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoDTO;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoResponse;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

// Convierte la entidad Repartidor.
// La entidad nunca se expone directamente en la API: el service la convierte aca
// antes de devolverla. Solo transforma datos, no aplica reglas de negocio
@Component
public class RepartidorMapper {

    public RepartidorResponse toResponse(Repartidor repartidor) {
        // getZona().getId() no carga la zona: el id ya esta en la columna zona_id
        Long zonaId = Objects.isNull(repartidor.getZona()) ? null : repartidor.getZona().getId();
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
                repartidor.getFechaAlta(),
                zonaId);
    }

    // CU-09: todos los vehiculos del repartidor, con su id y si estan activos
    public List<VehiculoResponse> toVehiculosResponse(Repartidor repartidor) {
        return repartidor.getVehiculos().stream()
                .map(v -> new VehiculoResponse(v.getId(), v.getTipo(), v.getPatente(),
                        v.getDescripcion(), v.isActivo()))
                .toList();
    }

    // DTO-09: se devuelve el vehiculo activo (REL-01); null si no tiene
    private VehiculoDTO toVehiculoDTO(Vehiculo vehiculo) {
        if (Objects.isNull(vehiculo)) {
            return null;
        }
        return new VehiculoDTO(vehiculo.getTipo(), vehiculo.getPatente(), vehiculo.getDescripcion());
    }
}
