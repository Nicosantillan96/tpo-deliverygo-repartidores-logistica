package ar.edu.uade.deliverygo.logistica.mapper;

import ar.edu.uade.deliverygo.logistica.dto.ZonaResponse;
import ar.edu.uade.deliverygo.logistica.model.Zona;
import org.springframework.stereotype.Component;

// Convierte la entidad Zona (CU-10). Solo transforma datos
@Component
public class ZonaMapper {

    public ZonaResponse toResponse(Zona zona) {
        return new ZonaResponse(zona.getId(), zona.getNombre());
    }
}
