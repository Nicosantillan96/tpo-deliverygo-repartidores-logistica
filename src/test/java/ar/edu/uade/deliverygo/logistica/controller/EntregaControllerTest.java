package ar.edu.uade.deliverygo.logistica.controller;

import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// API-03, API-04 y API-05 de punta a punta: HTTP -> controller -> service -> base H2.
// Los datos se arman con la misma API (alta del repartidor, disponibilidad, asignacion).
// @Transactional revierte los datos al terminar cada test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntregaRepository entregaRepository;

    // ---------- API-03 POST /api/v1/deliveries/assign ----------

    @Test
    void asignarConRepartidorDisponibleDevuelve201() throws Exception {
        repartidorDisponibleDelUsuario1();

        mockMvc.perform(post("/api/v1/deliveries/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(100L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.repartidorId").isNumber())
                // RN-13: el codigo nunca sale en la respuesta
                .andExpect(jsonPath("$.codigoConfirmacion").doesNotExist());
    }

    // DM-11: sin repartidores la entrega queda pendiente
    @Test
    void asignarSinRepartidoresDevuelve202() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(100L)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.repartidorId").isEmpty());
    }

    // RN-01 con @Valid
    @Test
    void asignarSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // RN-11: el pedido 106 del mock esta PENDIENTE
    @Test
    void asignarUnPedidoNoConfirmadoDevuelve422() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(106L)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CONFIRMED"));
    }

    // ---------- API-04 POST /api/v1/deliveries/{id}/pickup ----------

    @Test
    void elRepartidorAsignadoRegistraElRetiroSinBody() throws Exception {
        Long entregaId = entregaAsignadaAlUsuario1();

        mockMvc.perform(post("/api/v1/deliveries/{id}/pickup", entregaId)
                        .header("X-Usuario-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RETIRADA"))
                .andExpect(jsonPath("$.fechaRetiro").isNotEmpty());
    }

    // RN-10
    @Test
    void otroUsuarioNoPuedeRetirarYDevuelve403() throws Exception {
        Long entregaId = entregaAsignadaAlUsuario1();

        mockMvc.perform(post("/api/v1/deliveries/{id}/pickup", entregaId)
                        .header("X-Usuario-Id", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COURIER_NOT_ASSIGNED"));
    }

    @Test
    void retirarUnaEntregaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/{id}/pickup", 999)
                        .header("X-Usuario-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DELIVERY_NOT_FOUND"));
    }

    // ---------- API-05 POST /api/v1/deliveries/{id}/complete ----------

    @Test
    void confirmarConElCodigoCorrectoDevuelve200() throws Exception {
        Long entregaId = entregaAsignadaAlUsuario1();
        retirar(entregaId);

        mockMvc.perform(post("/api/v1/deliveries/{id}/complete", entregaId)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmacion(codigoDe(entregaId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.fechaEntrega").isNotEmpty());
    }

    // RN-09
    @Test
    void confirmarSinRetirarDevuelve409() throws Exception {
        Long entregaId = entregaAsignadaAlUsuario1();

        mockMvc.perform(post("/api/v1/deliveries/{id}/complete", entregaId)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmacion(codigoDe(entregaId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DELIVERY_NOT_PICKED_UP"));
    }

    // RN-13
    @Test
    void confirmarConUnCodigoIncorrectoDevuelve422() throws Exception {
        Long entregaId = entregaAsignadaAlUsuario1();
        retirar(entregaId);
        String incorrecto = "0000".equals(codigoDe(entregaId)) ? "1111" : "0000";

        mockMvc.perform(post("/api/v1/deliveries/{id}/complete", entregaId)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmacion(incorrecto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CONFIRMATION_CODE"));
    }

    // RN-01 con @Valid: el codigo es obligatorio
    @Test
    void confirmarSinCodigoDevuelve400() throws Exception {
        Long entregaId = entregaAsignadaAlUsuario1();

        mockMvc.perform(post("/api/v1/deliveries/{id}/complete", entregaId)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ---------- helpers ----------

    // Alta del repartidor del usuario 1 y lo pone DISPONIBLE, todo por la API
    private void repartidorDisponibleDelUsuario1() throws Exception {
        String json = mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": 1, "nombre": "Juan", "apellido": "Perez",
                                  "dni": "30123456", "telefono": "1155554444",
                                  "vehiculo": { "tipo": "BICICLETA" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long repartidorId = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(patch("/api/v1/couriers/{id}/availability", repartidorId)
                        .header("X-Usuario-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"disponibilidad\": \"DISPONIBLE\" }"))
                .andExpect(status().isOk());
    }

    private Long entregaAsignadaAlUsuario1() throws Exception {
        repartidorDisponibleDelUsuario1();
        String json = mockMvc.perform(post("/api/v1/deliveries/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(100L)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void retirar(Long entregaId) throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/{id}/pickup", entregaId)
                        .header("X-Usuario-Id", 1))
                .andExpect(status().isOk());
    }

    // El codigo no sale en la API (RN-13); en el test se lee de la base,
    // como si el cliente se lo dictara al repartidor
    private String codigoDe(Long entregaId) {
        return entregaRepository.findById(entregaId).orElseThrow().getCodigoConfirmacion();
    }

    private String pedido(Long pedidoId) {
        return """
                {
                  "pedidoId": %d, "clienteId": 200, "comercioId": 300,
                  "direccionRetiro": "Av. Corrientes 1234, CABA",
                  "direccionEntrega": "Lima 757, CABA"
                }
                """.formatted(pedidoId);
    }

    private String confirmacion(String codigo) {
        return "{ \"codigoConfirmacion\": \"" + codigo + "\" }";
    }
}
