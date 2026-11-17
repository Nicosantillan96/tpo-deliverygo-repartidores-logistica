package ar.edu.uade.deliverygo.logistica.config;

import ar.edu.uade.deliverygo.logistica.client.PedidosClientMock;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

    @Test
    void cargaTresRepartidoresYDosEntregas() {
        assertThat(repartidorRepository.count()).isEqualTo(3);
        assertThat(entregaRepository.count()).isEqualTo(2);
    }

    @Test
    void dejaUnaEntregaAsignadaYOtraRetirada() {
        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(PedidosClientMock.pedido(100), EstadoEntrega.CANCELADA))
                .get().extracting("estado").isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(PedidosClientMock.pedido(101), EstadoEntrega.CANCELADA))
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
}
