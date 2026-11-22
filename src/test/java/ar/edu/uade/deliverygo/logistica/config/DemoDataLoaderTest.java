package ar.edu.uade.deliverygo.logistica.config;

import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoResponse;
import ar.edu.uade.deliverygo.logistica.dto.ZonaResponse;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import ar.edu.uade.deliverygo.logistica.service.EntregaService;
import ar.edu.uade.deliverygo.logistica.service.RepartidorService;
import ar.edu.uade.deliverygo.logistica.service.ZonaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Arranca la aplicacion con el perfil "demo" y verifica los datos que deja el DemoDataLoader.
// Usa otra base H2 en memoria para no dejar datos en la que comparten los demas
// tests con @SpringBootTest (si no, el usuario 1 ya figuraria como repartidor)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1")
@ActiveProfiles("demo")
class DemoDataLoaderTest {

    @Autowired
    private RepartidorRepository repartidorRepository;

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private RepartidorService repartidorService;

    @Autowired
    private EntregaService entregaService;

    @Autowired
    private ZonaService zonaService;

    @Test
    void cargaTresRepartidoresYDosEntregas() {
        assertThat(repartidorRepository.count()).isEqualTo(3);
        assertThat(entregaRepository.count()).isEqualTo(2);
    }

    @Test
    void dejaUnaEntregaAsignadaYOtraRetirada() {
        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(100L, EstadoEntrega.CANCELADA))
                .get().extracting("estado").isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(101L, EstadoEntrega.CANCELADA))
                .get().extracting("estado").isEqualTo(EstadoEntrega.RETIRADA);
    }

    // Los dos con entrega quedan OCUPADO y el del auto DISPONIBLE para la proxima asignacion
    @Test
    void dejaUnRepartidorDisponible() {
        assertThat(repartidorRepository.findAll())
                .extracting("disponibilidad")
                .containsExactlyInAnyOrder(Disponibilidad.OCUPADO, Disponibilidad.OCUPADO,
                        Disponibilidad.DISPONIBLE);
    }

    @Test
    void lasEntregasTienenCodigoDeConfirmacion() {
        assertThat(entregaRepository.findAll())
                .allSatisfy(e -> assertThat(e.getCodigoConfirmacion()).matches("\\d{4}"));
    }

    // CU-10: dos zonas; el repartidor del auto (usuario 3) trabaja en Centro
    @Test
    void cargaDosZonasYElRepartidorDelAutoEstaEnCentro() {
        List<ZonaResponse> zonas = zonaService.listarZonas();
        assertThat(zonas).extracting(ZonaResponse::getNombre).containsExactly("Centro", "Palermo");

        RepartidorResponse auto = repartidorService.buscarPorId(idDelUsuario(3L));
        assertThat(auto.getZonaId()).isEqualTo(zonas.get(0).getId());
    }

    // CU-09: el repartidor del auto tiene el auto activo y una bicicleta inactiva
    @Test
    void elRepartidorDelAutoTieneUnaBicicletaInactiva() {
        List<VehiculoResponse> vehiculos = repartidorService.listarVehiculos(idDelUsuario(3L));

        assertThat(vehiculos).hasSize(2);
        assertThat(vehiculos).filteredOn(VehiculoResponse::isActivo)
                .extracting(VehiculoResponse::getTipo).containsExactly(TipoVehiculo.AUTO);
        assertThat(vehiculos).filteredOn(v -> !v.isActivo())
                .extracting(VehiculoResponse::getTipo).containsExactly(TipoVehiculo.BICICLETA);
    }

    // CU-08: la entrega retirada ya tiene su historial (creacion, asignacion y retiro)
    @Test
    void laEntregaRetiradaTieneSuHistorial() {
        Long retirada = entregaRepository.findByPedidoIdAndEstadoNot(101L, EstadoEntrega.CANCELADA)
                .orElseThrow().getId();

        assertThat(entregaService.consultarSeguimiento(retirada).getHistorial()).hasSize(3);
    }

    private Long idDelUsuario(Long usuarioId) {
        return repartidorRepository.findAll().stream()
                .filter(r -> usuarioId.equals(r.getUsuarioId()))
                .findFirst().orElseThrow().getId();
    }
}
