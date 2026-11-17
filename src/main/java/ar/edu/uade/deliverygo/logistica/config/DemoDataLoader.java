package ar.edu.uade.deliverygo.logistica.config;

import ar.edu.uade.deliverygo.logistica.client.PedidosClientMock;
import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRetiroRequest;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoDTO;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.service.EntregaService;
import ar.edu.uade.deliverygo.logistica.service.RepartidorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

// Datos de prueba para mostrar la API funcionando, como el DemoDataLoader del ejemplo de clase.
// Solo se carga con el perfil "demo" (en los tests y en el arranque normal no corre):
//   .\mvnw spring-boot:run "-Dspring-boot.run.profiles=demo"
// Usa los services, asi los datos pasan por las mismas reglas de negocio que la API.
//
// Estado que deja (usuarios y pedidos de los mocks):
// - Repartidor del usuario 1 (MOTO): entrega del pedido 100 ASIGNADA -> probar el retiro
// - Repartidor del usuario 2 (BICICLETA): entrega del pedido 101 RETIRADA -> probar la confirmacion
// - Repartidor del usuario 3 (AUTO): DISPONIBLE -> recibe la proxima asignacion (pedido 102 a 105)
// - Usuarios 4 y 5: libres para registrar repartidores nuevos
@Configuration
@Profile("demo")
public class DemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    // Pedidos del mock de Pedidos (los ids de Pedidos son UUID)
    private static final UUID PEDIDO_ASIGNADO = PedidosClientMock.pedido(100);
    private static final UUID PEDIDO_RETIRADO = PedidosClientMock.pedido(101);

    @Bean
    public CommandLineRunner cargarDatosDemo(RepartidorService repartidorService,
                                             EntregaService entregaService,
                                             EntregaRepository entregaRepository) {
        return args -> {
            // CU-01: tres repartidores, uno con cada tipo de vehiculo
            Long moto = registrar(repartidorService, 1L, "Juan", "Perez", "30111111",
                    new VehiculoDTO(TipoVehiculo.MOTO, "AB123CD", "Honda Wave"));
            Long bici = registrar(repartidorService, 2L, "Ana", "Gomez", "30222222",
                    new VehiculoDTO(TipoVehiculo.BICICLETA, null, "Bicicleta urbana"));
            Long auto = registrar(repartidorService, 3L, "Luis", "Diaz", "30333333",
                    new VehiculoDTO(TipoVehiculo.AUTO, "AC456EF", "Fiat Uno"));

            // CU-02: cada titular se pone DISPONIBLE (el usuarioId es el del propio repartidor)
            ponerDisponible(repartidorService, moto, 1L);
            ponerDisponible(repartidorService, bici, 2L);
            ponerDisponible(repartidorService, auto, 3L);

            // CU-03: el pedido 100 se asigna al repartidor de la moto
            EntregaResponse asignada = entregaService.asignarRepartidor(new AsignarEntregaRequest(
                    PEDIDO_ASIGNADO, 200L, 300L, "Av. Corrientes 1234, CABA", "Lima 757, CABA", moto));

            // CU-03 y CU-04: el pedido 101 se asigna al de la bicicleta, que ya lo retiro
            EntregaResponse retirada = entregaService.asignarRepartidor(new AsignarEntregaRequest(
                    PEDIDO_RETIRADO, 201L, 300L, "Av. Corrientes 1234, CABA", "Av. Santa Fe 3000, CABA", bici));
            retirada = entregaService.registrarRetiro(retirada.getId(),
                    new RegistrarRetiroRequest("Retirado en el mostrador"), 2L);

            log.info("========== Datos de demo cargados ==========");
            log.info("Repartidores: {} (usuario 1, MOTO), {} (usuario 2, BICICLETA), {} (usuario 3, AUTO)",
                    moto, bici, auto);
            log.info("Entrega {}: pedido {}, {}. Retiro con header X-Usuario-Id: 1. Codigo: {}",
                    asignada.getId(), asignada.getPedidoId(), asignada.getEstado(), codigo(entregaRepository, asignada.getId()));
            log.info("Entrega {}: pedido {}, {}. Confirmacion con header X-Usuario-Id: 2. Codigo: {}",
                    retirada.getId(), retirada.getPedidoId(), retirada.getEstado(), codigo(entregaRepository, retirada.getId()));
            log.info("Pedidos {} a {} libres para asignar; los toma el repartidor {}.",
                    PedidosClientMock.pedido(102), PedidosClientMock.pedido(105), auto);
            log.info("Swagger UI: http://localhost:8088/swagger-ui.html");
            log.info("============================================");
        };
    }

    private Long registrar(RepartidorService service, Long usuarioId, String nombre,
                           String apellido, String dni, VehiculoDTO vehiculo) {
        return service.registrarRepartidor(new RegistrarRepartidorRequest(
                usuarioId, nombre, apellido, dni, "1155554444", vehiculo)).getId();
    }

    private void ponerDisponible(RepartidorService service, Long repartidorId, Long usuarioId) {
        service.cambiarDisponibilidad(repartidorId,
                new CambiarDisponibilidadRequest(Disponibilidad.DISPONIBLE), usuarioId);
    }

    // El codigo no sale en EntregaResponse (RN-13). En la realidad lo recibe el cliente por
    // Notificaciones; en la demo se lee de la base solo para poder probar la confirmacion
    private String codigo(EntregaRepository repository, Long entregaId) {
        return repository.findById(entregaId).orElseThrow().getCodigoConfirmacion();
    }
}
