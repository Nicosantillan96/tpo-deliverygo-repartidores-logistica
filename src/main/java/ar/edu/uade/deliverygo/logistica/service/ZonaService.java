package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.dto.CrearZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.ZonaResponse;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.mapper.ZonaMapper;
import ar.edu.uade.deliverygo.logistica.model.Zona;
import ar.edu.uade.deliverygo.logistica.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// CU-10: administrar las zonas de operacion.
// Asignar una zona a un repartidor esta en RepartidorService.asignarZona
@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final ZonaMapper zonaMapper;

    public ZonaService(ZonaRepository zonaRepository, ZonaMapper zonaMapper) {
        this.zonaRepository = zonaRepository;
        this.zonaMapper = zonaMapper;
    }

    // SRV-17 / CU-10: alta de zona. El nombre no se repite (REP-12)
    @Transactional
    public ZonaResponse crearZona(CrearZonaRequest request) {
        String nombre = request.getNombre().trim();
        if (zonaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException(CodigoError.ZONE_NAME_DUPLICATED,
                    "Ya existe la zona " + nombre);
        }
        return zonaMapper.toResponse(zonaRepository.save(new Zona(nombre)));
    }

    // SRV-18 / CU-10: listado de zonas, por nombre (REP-13)
    @Transactional(readOnly = true)
    public List<ZonaResponse> listarZonas() {
        return zonaRepository.findAllByOrderByNombreAsc().stream()
                .map(zonaMapper::toResponse)
                .toList();
    }
}
