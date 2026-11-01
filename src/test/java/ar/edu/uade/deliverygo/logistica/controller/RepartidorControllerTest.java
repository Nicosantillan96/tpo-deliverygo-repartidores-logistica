package ar.edu.uade.deliverygo.logistica.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// API-01 y API-02 de punta a punta: HTTP -> controller -> service -> base H2.
// MockMvc simula las llamadas HTTP sin levantar un servidor real.
// @Transactional revierte los datos al terminar cada test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartidorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------- API-01 POST /api/v1/couriers ----------

    @Test
    void registrarDevuelve201ConElRepartidor() throws Exception {
        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(1L, "MOTO", "\"AB123CD\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.disponibilidad").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.dni").value("30123456"))
                .andExpect(jsonPath("$.vehiculo.patente").value("AB123CD"));
    }

    // RN-01 con @Valid: informa todos los campos que faltan
    @Test
    void registrarSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("nombre")))
                .andExpect(jsonPath("$.message", containsString("dni")))
                .andExpect(jsonPath("$.path").value("/api/v1/couriers"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    // RN-04
    @Test
    void registrarUnaMotoSinPatenteDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(1L, "MOTO", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEHICLE_PLATE_REQUIRED"));
    }

    // RN-02
    @Test
    void registrarUnDniRepetidoDevuelve409() throws Exception {
        registrar(1L);

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(2L, "BICICLETA", "null")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURIER_DNI_DUPLICATED"));
    }

    // RN-03: el usuario 7 del mock tiene rol CLIENTE
    @Test
    void registrarUnUsuarioNoElegibleDevuelve422() throws Exception {
        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(7L, "BICICLETA", "null")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("USER_NOT_ELIGIBLE"));
    }

    // ---------- API-02 PATCH /api/v1/couriers/{id}/availability ----------

    @Test
    void elTitularCambiaSuDisponibilidad() throws Exception {
        Long id = registrar(1L);

        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", id)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"disponibilidad\": \"DISPONIBLE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidad").value("DISPONIBLE"));
    }

    // RN-10: sin el header no se sabe quien hace el pedido
    @Test
    void cambiarDisponibilidadSinHeaderDevuelve400() throws Exception {
        Long id = registrar(1L);

        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"disponibilidad\": \"DISPONIBLE\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("X-Usuario-Id")));
    }

    // RN-10
    @Test
    void otroUsuarioNoPuedeCambiarlaYDevuelve403() throws Exception {
        Long id = registrar(1L);

        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", id)
                        .header("X-Usuario-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"disponibilidad\": \"DISPONIBLE\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // RN-01: @Valid sobre CambiarDisponibilidadRequest
    @Test
    void cambiarDisponibilidadSinValorDevuelve400() throws Exception {
        Long id = registrar(1L);

        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", id)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // Un valor que no existe en el enum (JSON con valor no permitido)
    @Test
    void cambiarDisponibilidadConValorInexistenteDevuelve400() throws Exception {
        Long id = registrar(1L);

        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", id)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"disponibilidad\": \"DE_VACACIONES\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cambiarDisponibilidadDeUnRepartidorInexistenteDevuelve404() throws Exception {
        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", 999)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"disponibilidad\": \"DISPONIBLE\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURIER_NOT_FOUND"));
    }

    // ---------- helpers ----------

    // Registra un repartidor por la API y devuelve su id
    private Long registrar(Long usuarioId) throws Exception {
        String json = mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta(usuarioId, "BICICLETA", "null")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    // patente va como literal JSON: "\"AB123CD\"" o "null"
    private String alta(Long usuarioId, String tipo, String patente) {
        return """
                {
                  "usuarioId": %d,
                  "nombre": "Juan",
                  "apellido": "Perez",
                  "dni": "30123456",
                  "telefono": "1155554444",
                  "vehiculo": { "tipo": "%s", "patente": %s }
                }
                """.formatted(usuarioId, tipo, patente);
    }
}
