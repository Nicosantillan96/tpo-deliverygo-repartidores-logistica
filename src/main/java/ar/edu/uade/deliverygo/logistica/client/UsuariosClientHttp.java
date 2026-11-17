package ar.edu.uade.deliverygo.logistica.client;

import ar.edu.uade.deliverygo.logistica.config.CorrelationIdFilter;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

// INT-01 real: Logistica -> Usuarios (Grupo 2). Reemplaza a UsuariosClientMock con el perfil "integracion".
// Contrato de Usuarios (verificado contra su servicio):
// 1. Logistica se identifica como modulo: POST /api/v1/auth/module-token {"modulo":"logistica","clave":...}
//    devuelve un JWT con rol MODULO y su vencimiento (expiraEn). Se guarda y se reutiliza hasta que vence.
// 2. GET /api/v1/users/{id}/access con ese token devuelve {"usuarioId", "estado", "rol"}; 404 si no existe.
// Si Usuarios no responde (o responde un error inesperado) se informa 503 USERS_SERVICE_UNAVAILABLE (CU-01 A6).
// Cada llamada reenvia el X-Correlation-Id del pedido en curso, para la trazabilidad
@Component
@Profile("integracion")
public class UsuariosClientHttp implements UsuariosClient {

    private static final Logger log = LoggerFactory.getLogger(UsuariosClientHttp.class);

    private static final String MODULO = "logistica";
    private static final String ESTADO_ACTIVO = "ACTIVO";
    // Se pide un token nuevo un poco antes de que venza, para no usar uno vencido en camino
    private static final Duration MARGEN_VENCIMIENTO = Duration.ofMinutes(1);

    private final RestClient restClient;
    private final String clave;

    // Token de modulo en uso y su vencimiento. synchronized: varios pedidos pueden pedirlo a la vez
    private String token;
    private Instant vence = Instant.EPOCH;

    // Constructor que usa Spring: agrega los tiempos de espera (2 s para conectar, 3 s para responder)
    @Autowired
    public UsuariosClientHttp(RestClient.Builder builder,
                              @Value("${logistica.usuarios.base-url}") String baseUrl,
                              @Value("${logistica.usuarios.clave-modulo}") String clave) {
        this(builder.requestFactory(tiemposDeEspera()), baseUrl, clave, true);
    }

    // Para los tests: usa el builder tal como viene (conectado a un servidor simulado)
    UsuariosClientHttp(RestClient.Builder builder, String baseUrl, String clave, boolean sinCambios) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.clave = clave;
    }

    private static SimpleClientHttpRequestFactory tiemposDeEspera() {
        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(Duration.ofSeconds(2));
        tiempos.setReadTimeout(Duration.ofSeconds(3));
        return tiempos;
    }

    @Override
    public UsuarioExterno buscarUsuario(Long usuarioId) {
        try {
            return consultarAcceso(usuarioId);
        } catch (HttpClientErrorException.Unauthorized e) {
            // El token pudo vencer o Usuarios se reinicio: se pide uno nuevo y se reintenta una vez
            invalidarToken();
            try {
                return consultarAcceso(usuarioId);
            } catch (HttpClientErrorException.Unauthorized otraVez) {
                throw noDisponible("Usuarios rechazó el token de módulo de Logística", otraVez);
            }
        }
    }

    private UsuarioExterno consultarAcceso(Long usuarioId) {
        try {
            AccesoUsuario acceso = restClient.get()
                    .uri("/api/v1/users/{id}/access", usuarioId)
                    .header("Authorization", "Bearer " + tokenVigente())
                    .header(CorrelationIdFilter.HEADER, CorrelationIdFilter.actual())
                    .retrieve()
                    .body(AccesoUsuario.class);
            if (Objects.isNull(acceso)) {
                throw noDisponible("Usuarios respondió sin datos", null);
            }
            return new UsuarioExterno(acceso.usuarioId(), ESTADO_ACTIVO.equals(acceso.estado()), acceso.rol());
        } catch (HttpClientErrorException.NotFound e) {
            // El usuario no existe en Usuarios: el service lo informa como USER_NOT_ELIGIBLE
            return null;
        } catch (HttpClientErrorException.Unauthorized e) {
            throw e;
        } catch (RestClientException e) {
            throw noDisponible("Usuarios no respondió al consultar el usuario " + usuarioId, e);
        }
    }

    private synchronized String tokenVigente() {
        if (Objects.isNull(token) || Instant.now().plus(MARGEN_VENCIMIENTO).isAfter(vence)) {
            try {
                TokenModulo respuesta = restClient.post()
                        .uri("/api/v1/auth/module-token")
                        .header(CorrelationIdFilter.HEADER, CorrelationIdFilter.actual())
                        .body(Map.of("modulo", MODULO, "clave", clave))
                        .retrieve()
                        .body(TokenModulo.class);
                if (Objects.isNull(respuesta) || Objects.isNull(respuesta.token())) {
                    throw noDisponible("Usuarios no devolvió el token de módulo", null);
                }
                token = respuesta.token();
                vence = Objects.isNull(respuesta.expiraEn()) ? Instant.now().plus(Duration.ofMinutes(5))
                        : respuesta.expiraEn();
            } catch (HttpClientErrorException.Unauthorized e) {
                // Clave de modulo incorrecta: es un error de configuracion, se avisa claro en el log
                throw noDisponible("Usuarios rechazó la clave de módulo de Logística (MODULE_KEY_LOGISTICA)", e);
            } catch (RestClientException e) {
                throw noDisponible("Usuarios no respondió al pedir el token de módulo", e);
            }
        }
        return token;
    }

    private synchronized void invalidarToken() {
        token = null;
    }

    private ReglaNegocioException noDisponible(String detalle, Exception causa) {
        log.error("[INT-01] {}", detalle, causa);
        return new ReglaNegocioException(CodigoError.USERS_SERVICE_UNAVAILABLE,
                "El módulo Usuarios no está disponible. Intente nuevamente más tarde");
    }

    // Respuestas de Usuarios: solo los campos que se usan; el resto se ignora
    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenModulo(String token, Instant expiraEn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccesoUsuario(Long usuarioId, String estado, String rol) {
    }
}
