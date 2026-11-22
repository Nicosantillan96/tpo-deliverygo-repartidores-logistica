package ar.edu.uade.deliverygo.logistica.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-10 de punta a punta: HTTP -> controller -> service -> base H2.
// @Transactional revierte los datos al terminar cada test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ZonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crearYListarZonas() throws Exception {
        crear("Palermo").andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Palermo"));

        mockMvc.perform(get("/api/v1/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Palermo"));
    }

    @Test
    void crearUnaZonaRepetidaDevuelve409() throws Exception {
        crear("Palermo").andExpect(status().isCreated());

        crear("Palermo").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ZONE_NAME_DUPLICATED"));
    }

    @Test
    void crearUnaZonaSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private org.springframework.test.web.servlet.ResultActions crear(String nombre) throws Exception {
        return mockMvc.perform(post("/api/v1/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nombre\": \"" + nombre + "\" }"));
    }
}
