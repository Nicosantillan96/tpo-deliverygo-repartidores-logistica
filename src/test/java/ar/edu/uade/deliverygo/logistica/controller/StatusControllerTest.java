package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.dto.StatusResponse;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import ar.edu.uade.deliverygo.logistica.service.StatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /status de punta a punta con la base H2 real
@SpringBootTest
@AutoConfigureMockMvc
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusDevuelve200ConElModuloYLaBaseUp() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.modulo").value("deliverygo-logistica"))
                .andExpect(jsonPath("$.baseDeDatos").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // Misma respuesta en la ruta que usan Usuarios y Analitica
    @Test
    void statusTambienRespondeEnApiV1() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // X-Correlation-Id: si viene, se devuelve el mismo; si no, se genera uno
    @Test
    void devuelveElCorrelationIdRecibido() throws Exception {
        mockMvc.perform(get("/status").header("X-Correlation-Id", "prueba-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "prueba-123"));
    }

    @Test
    void generaUnCorrelationIdSiNoViene() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    // El ErrorResponse usa el mismo correlationId que vino en el pedido
    @Test
    void elErrorUsaElCorrelationIdRecibido() throws Exception {
        mockMvc.perform(get("/api/v1/couriers/{id}", 999).header("X-Correlation-Id", "prueba-456"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", "prueba-456"))
                .andExpect(jsonPath("$.correlationId").value("prueba-456"));
    }

    // El Frontend puede consultarlo desde el navegador (CORS)
    @Test
    void statusPermiteOtroOrigen() throws Exception {
        mockMvc.perform(options("/status")
                        .header("Origin", "http://192.168.1.174:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.1.174:5173"));
    }

    // Si la base no responde, el modulo informa DOWN (el controller lo traduce a 503)
    @Test
    void siLaBaseFallaElEstadoEsDown() {
        StatusService service = new StatusService(repositorioQueFalla(), "deliverygo-logistica");

        StatusResponse estado = service.consultarEstado();

        assertThat(estado.getStatus()).isEqualTo("DOWN");
        assertThat(estado.getBaseDeDatos()).isEqualTo("DOWN");
    }

    @Test
    void siLaBaseFallaElControllerDevuelve503() {
        StatusController controller =
                new StatusController(new StatusService(repositorioQueFalla(), "deliverygo-logistica"));

        assertThat(controller.status().getStatusCode().value()).isEqualTo(503);
    }

    // Repositorio cuyo metodo count() simula una base caida
    private RepartidorRepository repositorioQueFalla() {
        return (RepartidorRepository) Proxy.newProxyInstance(
                RepartidorRepository.class.getClassLoader(),
                new Class<?>[]{RepartidorRepository.class},
                (proxy, metodo, args) -> {
                    throw new DataAccessResourceFailureException("La base no responde");
                });
    }
}
