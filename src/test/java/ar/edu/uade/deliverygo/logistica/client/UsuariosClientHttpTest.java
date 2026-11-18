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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// INT-01 real contra un Usuarios simulado (MockRestServiceServer): se verifica que consulte /access
// sin credencial y traduzca las respuestas
class UsuariosClientHttpTest {

    private static final String BASE = "http://usuarios";

    private MockRestServiceServer usuarios;
    private UsuariosClientHttp client;

    @BeforeEach
    void armar() {
        RestClient.Builder builder = RestClient.builder();
        usuarios = MockRestServiceServer.bindTo(builder).build();
        client = new UsuariosClientHttp(builder, BASE, true);
    }

    @Test
    void consultaElAccesoSinCredencialYLoTraduce() {
        usuarios.expect(requestTo(BASE + "/api/v1/users/4/access"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("Authorization"))
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
    void unUsuarioInactivoQuedaComoNoActivo() {
        usuarios.expect(requestTo(BASE + "/api/v1/users/5/access"))
                .andRespond(withSuccess("{\"usuarioId\":5,\"estado\":\"INACTIVO\",\"rol\":\"REPARTIDOR\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.buscarUsuario(5L).isActivo()).isFalse();
    }

    @Test
    void siElUsuarioNoExisteDevuelveNull() {
        usuarios.expect(requestTo(BASE + "/api/v1/users/999/access"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.buscarUsuario(999L)).isNull();
    }

    @Test
    void siUsuariosFallaInforma503() {
        usuarios.expect(requestTo(BASE + "/api/v1/users/4/access"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.buscarUsuario(4L))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.USERS_SERVICE_UNAVAILABLE);
    }
}
