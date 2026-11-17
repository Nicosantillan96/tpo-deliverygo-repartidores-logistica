package ar.edu.uade.deliverygo.logistica.client;

import ar.edu.uade.deliverygo.logistica.config.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// INT-05 real: Logistica -> Notificaciones (Grupo 1). Reemplaza a NotificacionesClientMock con el perfil
// "integracion". Contrato de Notificaciones (verificado contra su servicio):
//   POST /api/v1/notifications {"destinatarioId", "plantillaId", "canal", "parametros": {...}} -> 201
// - destinatarioId es el usuarioId de la persona (modulo Usuarios), no el id del repartidor.
// - Notificaciones solo acepta el JWT de una persona (CLIENTE, COMERCIO, REPARTIDOR, ADMIN), no el token
//   de modulo. Por eso se reenvia el header Authorization del pedido en curso (el JWT de quien hizo
//   la operacion: el repartidor en el retiro y la entrega). Si el pedido no trae token, el aviso falla.
// - Las plantillas las crea un ADMIN en Notificaciones; sus ids se configuran en
//   application-integracion.properties. Si una no esta configurada, ese aviso se omite.
// Los avisos no son bloqueantes (DM-12): si fallan, EntregaService lo registra en el log y la
// operacion sigue. Cada llamada reenvia el X-Correlation-Id del pedido en curso.
@Component
@Profile("integracion")
public class NotificacionesClientHttp implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientHttp.class);

    private final RestClient restClient;
    private final String canal;
    private final Long plantillaAsignacionCliente;
    private final Long plantillaAsignacionRepartidor;
    private final Long plantillaRetiro;
    private final Long plantillaEntrega;

    // Constructor que usa Spring: agrega los tiempos de espera (2 s para conectar, 3 s para responder)
    @Autowired
    public NotificacionesClientHttp(
            RestClient.Builder builder,
            @Value("${logistica.notificaciones.base-url}") String baseUrl,
            @Value("${logistica.notificaciones.canal:IN_APP}") String canal,
            @Value("${logistica.notificaciones.plantillas.asignacion-cliente:#{null}}") Long asignacionCliente,
            @Value("${logistica.notificaciones.plantillas.asignacion-repartidor:#{null}}") Long asignacionRepartidor,
            @Value("${logistica.notificaciones.plantillas.retiro:#{null}}") Long retiro,
            @Value("${logistica.notificaciones.plantillas.entrega:#{null}}") Long entrega) {
        this(builder.requestFactory(tiemposDeEspera()), baseUrl, canal,
                asignacionCliente, asignacionRepartidor, retiro, entrega, true);
    }

    // Para los tests: usa el builder tal como viene (conectado a un servidor simulado)
    NotificacionesClientHttp(RestClient.Builder builder, String baseUrl, String canal,
                             Long asignacionCliente, Long asignacionRepartidor, Long retiro, Long entrega,
                             boolean sinCambios) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.canal = canal;
        this.plantillaAsignacionCliente = asignacionCliente;
        this.plantillaAsignacionRepartidor = asignacionRepartidor;
        this.plantillaRetiro = retiro;
        this.plantillaEntrega = entrega;
    }

    // CU-03: el cliente recibe el codigo de confirmacion (RN-13) y el repartidor, la entrega asignada
    @Override
    public void avisarAsignacion(Long entregaId, Long usuarioRepartidorId, Long clienteId,
                                 String codigoConfirmacion) {
        enviar(clienteId, plantillaAsignacionCliente, "asignación (cliente)",
                Map.of("entregaId", String.valueOf(entregaId), "codigoConfirmacion", codigoConfirmacion));
        enviar(usuarioRepartidorId, plantillaAsignacionRepartidor, "asignación (repartidor)",
                Map.of("entregaId", String.valueOf(entregaId)));
    }

    // CU-04
    @Override
    public void avisarRetiro(Long entregaId, Long clienteId) {
        enviar(clienteId, plantillaRetiro, "retiro", Map.of("entregaId", String.valueOf(entregaId)));
    }

    // CU-05
    @Override
    public void avisarEntrega(Long entregaId, Long clienteId) {
        enviar(clienteId, plantillaEntrega, "entrega", Map.of("entregaId", String.valueOf(entregaId)));
    }

    private void enviar(Long destinatarioId, Long plantillaId, String aviso, Map<String, String> parametros) {
        if (Objects.isNull(plantillaId)) {
            log.warn("[INT-05] Aviso de {} omitido: falta configurar su plantilla de Notificaciones", aviso);
            return;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("destinatarioId", destinatarioId);
        cuerpo.put("plantillaId", plantillaId);
        cuerpo.put("canal", canal);
        cuerpo.put("parametros", parametros);

        RestClient.RequestBodySpec pedido = restClient.post()
                .uri("/api/v1/notifications")
                .header(CorrelationIdFilter.HEADER, CorrelationIdFilter.actual())
                .body(cuerpo);
        String authorization = authorizationDelPedidoEnCurso();
        if (Objects.nonNull(authorization)) {
            pedido.header("Authorization", authorization);
        }
        // Si Notificaciones responde un error, RestClient lanza una excepcion con el codigo y el cuerpo;
        // EntregaService.avisarSinRevertir la registra en el log sin revertir la operacion
        pedido.retrieve().toBodilessEntity();
        log.info("[INT-05] Aviso de {} enviado a Notificaciones (destinatario {}, plantilla {})",
                aviso, destinatarioId, plantillaId);
    }

    // Header Authorization del pedido HTTP que se esta atendiendo, o null si no hay
    private String authorizationDelPedidoEnCurso() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getHeader("Authorization");
        }
        return null;
    }

    private static SimpleClientHttpRequestFactory tiemposDeEspera() {
        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(Duration.ofSeconds(2));
        tiempos.setReadTimeout(Duration.ofSeconds(3));
        return tiempos;
    }
}
