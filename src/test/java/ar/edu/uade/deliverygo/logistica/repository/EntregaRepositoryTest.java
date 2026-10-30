package ar.edu.uade.deliverygo.logistica.repository;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.HistorialEstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EntregaRepositoryTest {

    // Estados en los que una entrega cuenta como activa (RN-07)
    private static final List<EstadoEntrega> ACTIVAS =
            List.of(EstadoEntrega.ASIGNADA, EstadoEntrega.RETIRADA, EstadoEntrega.EN_CAMINO);

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private RepartidorRepository repartidorRepository;

    @Autowired
    private TestEntityManager entityManager;

    // REP-05 (RN-07)
    @Test
    void existsEntregaActivaDelRepartidor() {
        Repartidor conEntrega = repartidorRepository.save(repartidor(1L, "1"));
        Repartidor libre = repartidorRepository.save(repartidor(2L, "2"));
        entregaRepository.save(entrega(100L, EstadoEntrega.RETIRADA, conEntrega));
        // Una entrega COMPLETADA no cuenta como activa
        entregaRepository.save(entrega(200L, EstadoEntrega.COMPLETADA, libre));

        assertThat(entregaRepository.existsByRepartidorIdAndEstadoIn(conEntrega.getId(), ACTIVAS)).isTrue();
        assertThat(entregaRepository.existsByRepartidorIdAndEstadoIn(libre.getId(), ACTIVAS)).isFalse();
    }

    // REP-06 (RN-11, DM-11): ignora las canceladas y devuelve la vigente
    @Test
    void findByPedidoIdIgnoraLasCanceladas() {
        entregaRepository.save(entrega(100L, EstadoEntrega.CANCELADA, null));
        Entrega vigente = entregaRepository.save(entrega(100L, EstadoEntrega.PENDIENTE_ASIGNACION, null));
        entregaRepository.save(entrega(200L, EstadoEntrega.CANCELADA, null));

        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(100L, EstadoEntrega.CANCELADA))
                .get().extracting(Entrega::getId).isEqualTo(vigente.getId());
        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(200L, EstadoEntrega.CANCELADA))
                .isEmpty();
        assertThat(entregaRepository.findByPedidoIdAndEstadoNot(300L, EstadoEntrega.CANCELADA))
                .isEmpty();
    }

    private Entrega entrega(Long pedidoId, EstadoEntrega estado, Repartidor repartidor) {
        Entrega entrega = new Entrega();
        entrega.setPedidoId(pedidoId);
        entrega.setClienteId(200L);
        entrega.setComercioId(300L);
        entrega.setDireccionRetiro("Av. Corrientes 1234, CABA");
        entrega.setDireccionEntrega("Lima 757, CABA");
        entrega.setEstado(estado);
        entrega.setFechaCreacion(LocalDateTime.now());
        entrega.setRepartidor(repartidor);
        return entrega;
    }

    private Repartidor repartidor(Long usuarioId, String dni) {
        Repartidor repartidor = new Repartidor();
        repartidor.setUsuarioId(usuarioId);
        repartidor.setNombre("Juan");
        repartidor.setApellido("Perez");
        repartidor.setDni(dni);
        repartidor.setTelefono("1155554444");
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.OCUPADO);
        repartidor.setFechaAlta(LocalDateTime.now());
        return repartidor;
    }

    @Test
    void guardarEntregaGuardaSuHistorialEnCascada() {
        Entrega entrega = new Entrega();
        entrega.setPedidoId(100L);
        entrega.setClienteId(200L);
        entrega.setComercioId(300L);
        entrega.setDireccionRetiro("Av. Corrientes 1234, CABA");
        entrega.setDireccionEntrega("Lima 757, CABA");
        entrega.setEstado(EstadoEntrega.PENDIENTE_ASIGNACION);
        entrega.setFechaCreacion(LocalDateTime.now());

        // Registro inicial: no tiene estado anterior
        HistorialEstadoEntrega registro = new HistorialEstadoEntrega();
        registro.setEstadoNuevo(EstadoEntrega.PENDIENTE_ASIGNACION);
        registro.setFechaHora(LocalDateTime.now());
        registro.setUsuarioActorId(1L);
        entrega.agregarHistorial(registro);

        // Solo se guarda la entrega; el historial debe guardarse por cascada
        Long id = entregaRepository.save(entrega).getId();

        // flush: ejecuta los INSERT. clear: vacia la memoria de JPA,
        // asi la lectura siguiente sale realmente de la base de datos
        entityManager.flush();
        entityManager.clear();

        Entrega leida = entregaRepository.findById(id).orElseThrow();

        assertThat(leida.getPedidoId()).isEqualTo(100L);
        assertThat(leida.getCodigoConfirmacion()).isNull();
        assertThat(leida.getHistorial()).hasSize(1);

        HistorialEstadoEntrega registroLeido = leida.getHistorial().get(0);
        assertThat(registroLeido.getId()).isNotNull();
        assertThat(registroLeido.getEstadoAnterior()).isNull();
        assertThat(registroLeido.getEstadoNuevo()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
    }
}
