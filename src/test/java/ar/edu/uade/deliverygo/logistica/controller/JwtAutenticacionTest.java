package ar.edu.uade.deliverygo.logistica.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RN-10 con JWT: la identidad sale del claim sub del token de Usuarios (HS256).
// Los tokens se arman en el test con el mismo algoritmo que el emisor.
// Se usa CU-02 como endpoint de prueba: sin repartidor registrado, un usuario autenticado
// llega al service y recibe 404 COURIER_NOT_FOUND; eso prueba que la identidad se resolvio
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "logistica.jwt.secret=secreto-de-prueba-para-los-tests-0123456789")
class JwtAutenticacionTest {

    private static final String SECRETO = "secreto-de-prueba-para-los-tests-0123456789";
    private static final String BODY = "{\"disponibilidad\":\"DISPONIBLE\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tokenValidoIdentificaAlUsuario() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .header("Authorization", "Bearer " + token("1", futuro(), SECRETO))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURIER_NOT_FOUND"));
    }

    @Test
    void firmaMalaDevuelve401() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .header("Authorization", "Bearer " + token("1", futuro(), "otro-secreto-distinto"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .header("Authorization", "Bearer " + token("1", Instant.now().getEpochSecond() - 60, SECRETO))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void subNoNumericoDevuelve401() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .header("Authorization", "Bearer " + token("abc", futuro(), SECRETO))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConFormatoInvalidoDevuelve401() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .header("Authorization", "Bearer basura")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinHeadersDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("X-Usuario-Id")))
                .andExpect(jsonPath("$.message", containsString("Authorization")));
    }

    @Test
    void xUsuarioIdSigueFuncionando() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/999/availability")
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    private static long futuro() {
        return Instant.now().getEpochSecond() + 3600;
    }

    // Mismo formato que el emisor: header.payload.firma en Base64URL sin padding
    private static String token(String sub, long exp, String secreto) throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"sub\":\"" + sub + "\",\"role\":\"REPARTIDOR\",\"iat\":1,\"exp\":"
                + exp + "}").getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = b64.encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return header + "." + payload + "." + firma;
    }
}
