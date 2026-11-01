package ar.edu.uade.deliverygo.logistica.excepcion;

import org.springframework.http.HttpStatus;

// Codigos de error de la especificacion (seccion 17), cada uno con su codigo HTTP.
// El GlobalExceptionHandler usa getStatus() para armar la respuesta, asi que agregar
// un error nuevo es solo agregar una linea aca.
public enum CodigoError {

    // 400: datos faltantes o con formato invalido
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    VEHICLE_PLATE_REQUIRED(HttpStatus.BAD_REQUEST),

    // 403: el usuario no es el titular o no es el repartidor asignado (RN-10)
    FORBIDDEN(HttpStatus.FORBIDDEN),
    COURIER_NOT_ASSIGNED(HttpStatus.FORBIDDEN),

    // 404: recurso inexistente
    COURIER_NOT_FOUND(HttpStatus.NOT_FOUND),
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),

    // 409: conflicto con el estado actual de los datos
    COURIER_DNI_DUPLICATED(HttpStatus.CONFLICT),
    COURIER_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    COURIER_HAS_ACTIVE_DELIVERY(HttpStatus.CONFLICT),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT),
    DELIVERY_INVALID_STATE(HttpStatus.CONFLICT),
    DELIVERY_NOT_PICKED_UP(HttpStatus.CONFLICT),
    // No esta en la seccion 17: CU-03 A6 pide 409 sin nombrar un codigo.
    // Lo usa el handler cuando @Version detecta dos cambios simultaneos (DM-09)
    CONCURRENT_UPDATE(HttpStatus.CONFLICT),

    // 422: datos validos pero que no cumplen una regla de negocio
    USER_NOT_ELIGIBLE(HttpStatus.UNPROCESSABLE_ENTITY),
    COURIER_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY),
    COURIER_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY),
    ORDER_NOT_CONFIRMED(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_CONFIRMATION_CODE(HttpStatus.UNPROCESSABLE_ENTITY);

    private final HttpStatus status;

    CodigoError(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
