package ar.edu.uade.deliverygo.logistica.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

// INT-05 real contra un Notificaciones simulado (MockRestServiceServer)
class NotificacionesClientHttpTest {

    private static final String BASE = "http://notificaciones";
    private static final String URL = BASE + "/api/v1/notifications";

    private MockRestServiceServer notificaciones;
    private NotificacionesClientHttp client;

    @BeforeEach
    void armar() {
        RestClient.Builder builder = RestClient.builder();
        notificaciones = MockRestServiceServer.bindTo(builder).build();
        client = new NotificacionesClientHttp(builder, BASE, "IN_APP", 6L, 7L, 8L, 9L, true);
        // Simula el pedido HTTP en curso: el repartidor llamo con su JWT
        MockHttpServletRequest pedido = new MockHttpServletRequest();
        pedido.addHeader("Authorization", "Bearer jwt-del-repartidor");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(pedido));
    }

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    // CU-03: al cliente le llega el codigo; al repartidor, por su usuarioId, la entrega asignada
    @Test
    void laAsignacionAvisaAlClienteConElCodigoYAlRepartidor() {
        notificaciones.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer jwt-del-repartidor"))
                .andExpect(header("X-Correlation-Id", org.hamcrest.Matchers.notNullValue()))
                .andExpect(content().json("""
                        { "destinatarioId": 10, "plantillaId": 6, "canal": "IN_APP",
                          "parametros": { "entregaId": "1", "codigoConfirmacion": "4821" } }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));
        notificaciones.expect(requestTo(URL))
                .andExpect(content().json("""
                        { "destinatarioId": 3, "plantillaId": 7, "parametros": { "entregaId": "1" } }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.avisarAsignacion(1L, 3L, 10L, "4821");

        notificaciones.verify();
    }

    @Test
    void elRetiroYLaEntregaAvisanAlCliente() {
        notificaciones.expect(requestTo(URL))
                .andExpect(content().json("{ \"destinatarioId\": 10, \"plantillaId\": 8 }"))
                .andRespond(withStatus(HttpStatus.CREATED));
        notificaciones.expect(requestTo(URL))
                .andExpect(content().json("{ \"destinatarioId\": 10, \"plantillaId\": 9 }"))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.avisarRetiro(1L, 10L);
        client.avisarEntrega(1L, 10L);

        notificaciones.verify();
    }

    // Si Notificaciones rechaza el aviso se lanza una excepcion; EntregaService la registra sin revertir
    @Test
    void siNotificacionesRechazaElAvisoLanzaExcepcion() {
        notificaciones.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.avisarRetiro(1L, 10L)).isInstanceOf(RestClientException.class);
    }

    // Una plantilla sin configurar: ese aviso se omite y no se llama a Notificaciones
    @Test
    void sinPlantillaConfiguradaNoLlama() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        NotificacionesClientHttp sinPlantillas =
                new NotificacionesClientHttp(builder, BASE, "IN_APP", null, null, null, null, true);
        servidor.expect(never(), requestTo(URL));

        sinPlantillas.avisarEntrega(1L, 10L);

        servidor.verify();
    }
}
