package ar.edu.uade.deliverygo.logistica.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// springdoc publica el contrato OpenAPI con los cinco endpoints del modulo
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elContratoOpenApiIncluyeLosCincoEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DeliveryGo - Repartidores y Logística"))
                .andExpect(jsonPath("$.paths['/api/v1/couriers'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/couriers/{id}/availability'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/deliveries/assign'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/deliveries/{id}/pickup'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/deliveries/{id}/complete'].post").exists());
    }

    @Test
    void swaggerUiEstaDisponible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
