package ar.edu.uade.deliverygo.logistica.config;

import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// RN-10: obtiene el usuarioId para los parametros @UsuarioAutenticado, asi el controller
// no sabe de donde viene la identidad.
// 1) Authorization: Bearer <JWT de Usuarios> (si hay secreto configurado) -> "sub" del token
// 2) X-Usuario-Id (simplificacion usada en tests y demo)
// 3) ninguno -> 400 VALIDATION_ERROR
@Component
public class UsuarioAutenticadoResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER_USUARIO = "X-Usuario-Id";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtValidador jwtValidador;

    public UsuarioAutenticadoResolver(JwtValidador jwtValidador) {
        this.jwtValidador = jwtValidador;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UsuarioAutenticado.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String authorization = webRequest.getHeader("Authorization");
        if (jwtValidador.habilitado() && authorization != null
                && authorization.regionMatches(true, 0, PREFIJO_BEARER, 0, PREFIJO_BEARER.length())) {
            return jwtValidador.validar(authorization.substring(PREFIJO_BEARER.length()).trim());
        }

        String header = webRequest.getHeader(HEADER_USUARIO);
        if (header == null || header.isBlank()) {
            throw new ReglaNegocioException(CodigoError.VALIDATION_ERROR,
                    "Falta identificar al usuario: enviar el header Authorization (Bearer) o " + HEADER_USUARIO);
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException(CodigoError.VALIDATION_ERROR,
                    "El header " + HEADER_USUARIO + " debe ser un número");
        }
    }
}
