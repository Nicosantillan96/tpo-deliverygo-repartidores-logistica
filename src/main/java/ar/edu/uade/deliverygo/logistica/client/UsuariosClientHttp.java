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
import java.util.Objects;

// INT-01 real: Logistica -> Usuarios (Grupo 2). Reemplaza a UsuariosClientMock con el perfil "integracion".
// Contrato de Usuarios (verificado contra su servicio):
//   GET /api/v1/users/{id}/access -> {"usuarioId", "estado", "rol"}; 404 si el usuario no existe.
// Usuarios decidio que los modulos lo consultan sin credencial (las lecturas entre modulos son publicas;
// el JWT queda solo para el Frontend), asi que no se manda token.
// Si Usuarios no responde (o responde un error inesperado) se informa 503 USERS_SERVICE_UNAVAILABLE (CU-01 A6).
// Cada llamada reenvia el X-Correlation-Id del pedido en curso, para la trazabilidad
@Component
@Profile("integracion")
public class UsuariosClientHttp implements UsuariosClient {

    private static final Logger log = LoggerFactory.getLogger(UsuariosClientHttp.class);

    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final RestClient restClient;

    // Constructor que usa Spring: agrega los tiempos de espera (2 s para conectar, 3 s para responder)
    @Autowired
    public UsuariosClientHttp(RestClient.Builder builder,
                              @Value("${logistica.usuarios.base-url}") String baseUrl) {
        this(builder.requestFactory(tiemposDeEspera()), baseUrl, true);
    }

    // Para los tests: usa el builder tal como viene (conectado a un servidor simulado)
    UsuariosClientHttp(RestClient.Builder builder, String baseUrl, boolean sinCambios) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public UsuarioExterno buscarUsuario(Long usuarioId) {
        try {
            AccesoUsuario acceso = restClient.get()
                    .uri("/api/v1/users/{id}/access", usuarioId)
                    .header(CorrelationIdFilter.HEADER, CorrelationIdFilter.actual())
                    .retrieve()
                    .body(AccesoUsuario.class);
            if (Objects.isNull(acceso)) {
                throw noDisponible("Usuarios respondió sin datos para el usuario " + usuarioId, null);
            }
            return new UsuarioExterno(acceso.usuarioId(), ESTADO_ACTIVO.equals(acceso.estado()), acceso.rol());
        } catch (HttpClientErrorException.NotFound e) {
            // El usuario no existe en Usuarios: el service lo informa como USER_NOT_ELIGIBLE
            return null;
        } catch (RestClientException e) {
            throw noDisponible("Usuarios no respondió al consultar el usuario " + usuarioId, e);
        }
    }

    private ReglaNegocioException noDisponible(String detalle, Exception causa) {
        log.error("[INT-01] {}", detalle, causa);
        return new ReglaNegocioException(CodigoError.USERS_SERVICE_UNAVAILABLE,
                "El módulo Usuarios no está disponible. Intente nuevamente más tarde");
    }

    private static SimpleClientHttpRequestFactory tiemposDeEspera() {
        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(Duration.ofSeconds(2));
        tiempos.setReadTimeout(Duration.ofSeconds(3));
        return tiempos;
    }

    // Respuesta de Usuarios: solo los campos que se usan; el resto se ignora
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccesoUsuario(Long usuarioId, String estado, String rol) {
    }
}
