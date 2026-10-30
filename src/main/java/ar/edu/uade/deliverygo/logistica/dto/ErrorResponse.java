package ar.edu.uade.deliverygo.logistica.dto;

import java.time.LocalDateTime;

// DTO-08: formato normalizado de error del ecosistema (seccion 17)
// code lleva el codigo de negocio, por ejemplo DELIVERY_NOT_PICKED_UP
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final String correlationId;

    public ErrorResponse(LocalDateTime timestamp, int status, String code,
                         String message, String path, String correlationId) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
    }

    //GETTERS
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
