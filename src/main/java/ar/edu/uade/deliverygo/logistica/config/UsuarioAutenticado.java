package ar.edu.uade.deliverygo.logistica.config;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marca el parametro Long de un controller que debe recibir el id del usuario autenticado (RN-10).
// UsuarioAutenticadoResolver lo calcula desde el JWT de Usuarios o, para pruebas, desde X-Usuario-Id.
// Se oculta en Swagger porque no es un parametro que mande el cliente: los headers reales
// se documentan a nivel metodo en cada endpoint
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
public @interface UsuarioAutenticado {
}
