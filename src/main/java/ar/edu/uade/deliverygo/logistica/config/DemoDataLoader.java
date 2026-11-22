package ar.edu.uade.deliverygo.logistica.config;

import ar.edu.uade.deliverygo.logistica.dto.AgregarVehiculoRequest;
import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.AsignarZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.CrearZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRetiroRequest;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoDTO;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoResponse;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.service.EntregaService;
import ar.edu.uade.deliverygo.logistica.service.RepartidorService;
import ar.edu.uade.deliverygo.logistica.service.ZonaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

// Datos de prueba para mostrar la API funcionando, como el DemoDataLoader del ejemplo de clase.
// Solo se carga con el perfil "demo" (en los tests y en el arranque normal no corre):
//   .\mvnw spring-boot:run "-Dspring-boot.run.profiles=demo"
// Usa los services, asi los datos pasan por las mismas reglas de negocio que la API.
//
// Estado que deja (usuarios y pedidos de los mocks), con el CU que se puede probar:
// - Repartidor del usuario 1 (MOTO): entrega del pedido 100 ASIGNADA
//     -> CU-06 aceptar o rechazar, CU-04 retiro
// - Repartidor del usuario 2 (BICICLETA): entrega del pedido 101 RETIRADA
//     -> CU-07 en camino, CU-05 confirmacion
// - Repartidor del usuario 3 (AUTO): DISPONIBLE, en la zona Centro, con una bicicleta inactiva
//     -> recibe la proxima asignacion (pedidos 102 a 105), CU-09 vehiculos, CU-10 zona
// - Zonas Centro y Palermo (CU-10): en Palermo no hay repartidores
// - Cualquier entrega -> CU-08 seguimiento con su historial
// - Usuarios 4 y 5: libres para registrar repartidores nuevos
@Configuration
@Profile("demo")
public class DemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    @Bean
    public CommandLineRunner cargarDatosDemo(RepartidorService repartidorService,
                                             EntregaService entregaService,
                                             ZonaService zonaService,
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

            // CU-10: dos zonas; el repartidor del auto trabaja en Centro
            Long centro = zonaService.crearZona(new CrearZonaRequest("Centro")).getId();
            Long palermo = zonaService.crearZona(new CrearZonaRequest("Palermo")).getId();
            repartidorService.asignarZona(auto, new AsignarZonaRequest(centro), 3L);

            // CU-09: el repartidor del auto suma una bicicleta, que queda inactiva
            List<VehiculoResponse> vehiculosAuto = repartidorService.agregarVehiculo(auto,
                    new AgregarVehiculoRequest(TipoVehiculo.BICICLETA, null, "Bicicleta de repuesto"), 3L);
            Long biciDeRepuesto = vehiculosAuto.stream()
                    .filter(v -> !v.isActivo())
                    .findFirst().orElseThrow().getId();

            // CU-03: el pedido 100 se asigna al repartidor de la moto
            EntregaResponse asignada = entregaService.asignarRepartidor(new AsignarEntregaRequest(
                    100L, 200L, 300L, "Av. Corrientes 1234, CABA", "Lima 757, CABA", moto));

            // CU-03 y CU-04: el pedido 101 se asigna al de la bicicleta, que ya lo retiro
            EntregaResponse retirada = entregaService.asignarRepartidor(new AsignarEntregaRequest(
                    101L, 201L, 300L, "Av. Corrientes 1234, CABA", "Av. Santa Fe 3000, CABA", bici));
            retirada = entregaService.registrarRetiro(retirada.getId(),
                    new RegistrarRetiroRequest("Retirado en el mostrador"), 2L);

            log.info("========== Datos de demo cargados ==========");
            log.info("Repartidores: {} (usuario 1, MOTO), {} (usuario 2, BICICLETA), {} (usuario 3, AUTO)",
                    moto, bici, auto);
            log.info("Entrega {}: pedido 100, {}. Con header X-Usuario-Id: 1 se puede aceptar o "
                            + "rechazar (CU-06) o retirar (CU-04). Codigo: {}",
                    asignada.getId(), asignada.getEstado(), codigo(entregaRepository, asignada.getId()));
            log.info("Entrega {}: pedido 101, {}. Con header X-Usuario-Id: 2 se puede informar en "
                            + "camino (CU-07) y confirmar (CU-05). Codigo: {}",
                    retirada.getId(), retirada.getEstado(), codigo(entregaRepository, retirada.getId()));
            log.info("Seguimiento (CU-08): GET /api/v1/deliveries/{} o /{}", asignada.getId(), retirada.getId());
            log.info("Pedidos 102 a 105 libres para asignar; los toma el repartidor {}. "
                    + "Con zonaId {} (Centro) tambien; con zonaId {} (Palermo) quedan pendientes (CU-10).",
                    auto, centro, palermo);
            log.info("Vehiculos (CU-09) del repartidor {} con header X-Usuario-Id: 3. "
                    + "Bicicleta inactiva: vehiculo {}", auto, biciDeRepuesto);
            log.info("Swagger UI: http://localhost:8080/swagger-ui.html");
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
