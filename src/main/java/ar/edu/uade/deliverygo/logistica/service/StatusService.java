package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.dto.StatusResponse;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Estado del modulo para la integracion (GET /status).
// Comprueba que la base responde haciendo una consulta simple (count de repartidores).
// No es un caso de uso ni aplica reglas de negocio
@Service
public class StatusService {

    private static final Logger log = LoggerFactory.getLogger(StatusService.class);

    private final RepartidorRepository repartidorRepository;
    private final String modulo;

    public StatusService(RepartidorRepository repartidorRepository,
                         @Value("${spring.application.name}") String modulo) {
        this.repartidorRepository = repartidorRepository;
        this.modulo = modulo;
    }

    public StatusResponse consultarEstado() {
        String baseDeDatos = StatusResponse.UP;
        try {
            repartidorRepository.count();
        } catch (RuntimeException e) {
            log.error("GET /status: la base de datos no responde", e);
            baseDeDatos = StatusResponse.DOWN;
        }
        // El modulo esta UP solo si su base responde
        return new StatusResponse(baseDeDatos, modulo, baseDeDatos, LocalDateTime.now());
    }
}
