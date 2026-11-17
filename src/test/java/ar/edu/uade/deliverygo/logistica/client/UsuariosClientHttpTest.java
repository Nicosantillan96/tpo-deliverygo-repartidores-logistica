package ar.edu.uade.deliverygo.logistica.client;

import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// INT-01 real contra un Usuarios simulado (MockRestServiceServer): se verifica que pida el token
// de modulo, consulte /access con ese token y traduzca las respuestas
class UsuariosClientHttpTest {

    private static final String BASE = "http://usuarios";
    private static final String TOKEN = """
            { "token": "token-modulo", "tipo": "Bearer", "expiraEn": "2999-01-01T00:00:00Z",
              "modulo": "logistica", "rol": "MODULO" }
            """;

    private MockRestServiceServer usuarios;
    private UsuariosClientHttp client;

    @BeforeEach
    void armar() {
        RestClient.Builder builder = RestClient.builder();
        usuarios = MockRestServiceServer.bindTo(builder).build();
        client = new UsuariosClientHttp(builder, BASE, "clave-de-prueba-1234567", true);
    }

    @Test
    void pideElTokenYTraduceElAcceso() {
        esperarToken();
        usuarios.expect(once(), requestTo(BASE + "/api/v1/users/4/access"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-modulo"))
                .andExpect(header("X-Correlation-Id", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess("""
                        { "usuarioId": 4, "estado": "ACTIVO", "rol": "CLIENTE" }
                        """, MediaType.APPLICATION_JSON));

        UsuarioExterno usuario = client.buscarUsuario(4L);

        assertThat(usuario.getId()).isEqualTo(4L);
        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getRol()).isEqualTo("CLIENTE");
        usuarios.verify();
    }

    @Test
    void reutilizaElTokenEnLaSegundaConsulta() {
        esperarToken();
        usuarios.expect(requestTo(BASE + "/api/v1/users/4/access"))
                .andRespond(withSuccess("{\"usuarioId\":4,\"estado\":\"INACTIVO\",\"rol\":\"REPARTIDOR\"}",
                        MediaType.APPLICATION_JSON));
        usuarios.expect(requestTo(BASE + "/api/v1/users/5/access"))
                .andExpect(header("Authorization", "Bearer token-modulo"))
                .andRespond(withSuccess("{\"usuarioId\":5,\"estado\":\"ACTIVO\",\"rol\":\"REPARTIDOR\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.buscarUsuario(4L).isActivo()).isFalse();
        assertThat(client.buscarUsuario(5L).getRol()).isEqualTo("REPARTIDOR");
        usuarios.verify();
    }

    @Test
    void siElUsuarioNoExisteDevuelveNull() {
        esperarToken();
        usuarios.expect(requestTo(BASE + "/api/v1/users/999/access"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.buscarUsuario(999L)).isNull();
    }

    @Test
    void siUsuariosFallaInforma503() {
        esperarToken();
        usuarios.expect(requestTo(BASE + "/api/v1/users/4/access"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.buscarUsuario(4L))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.USERS_SERVICE_UNAVAILABLE);
    }

    @Test
    void siLaClaveDeModuloEsIncorrectaInforma503() {
        usuarios.expect(requestTo(BASE + "/api/v1/auth/module-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.buscarUsuario(4L))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.USERS_SERVICE_UNAVAILABLE);
    }

    // Si Usuarios rechaza el token (vencio o se reinicio), pide uno nuevo y reintenta una vez
    @Test
    void siElTokenFueRechazadoPideOtroYReintenta() {
        esperarToken();
        usuarios.expect(requestTo(BASE + "/api/v1/users/4/access"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        esperarToken();
        usuarios.expect(requestTo(BASE + "/api/v1/users/4/access"))
                .andRespond(withSuccess("{\"usuarioId\":4,\"estado\":\"ACTIVO\",\"rol\":\"REPARTIDOR\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.buscarUsuario(4L).getRol()).isEqualTo("REPARTIDOR");
        usuarios.verify();
    }

    private void esperarToken() {
        usuarios.expect(requestTo(BASE + "/api/v1/auth/module-token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(content().json("{\"modulo\":\"logistica\",\"clave\":\"clave-de-prueba-1234567\"}"))
                .andRespond(withSuccess(TOKEN, MediaType.APPLICATION_JSON));
    }
}
