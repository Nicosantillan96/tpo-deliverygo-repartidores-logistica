package ar.edu.uade.deliverygo.logistica.dto;

import java.time.LocalDateTime;

// Salida de GET /status: permite a los demas equipos confirmar que el modulo esta levantado.
// Solo lectura: no tiene setters
public class StatusResponse {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    private final String status;
    private final String modulo;
    private final String baseDeDatos;
    private final LocalDateTime timestamp;

    public StatusResponse(String status, String modulo, String baseDeDatos, LocalDateTime timestamp) {
        this.status = status;
        this.modulo = modulo;
        this.baseDeDatos = baseDeDatos;
        this.timestamp = timestamp;
    }

    //GETTERS
    public String getStatus() {
        return status;
    }

    public String getModulo() {
        return modulo;
    }

    public String getBaseDeDatos() {
        return baseDeDatos;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
