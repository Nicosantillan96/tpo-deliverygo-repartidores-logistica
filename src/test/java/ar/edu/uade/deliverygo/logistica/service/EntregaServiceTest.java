package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.NotificacionesClientMock;
import ar.edu.uade.deliverygo.logistica.client.PedidosClientMock;
import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.HistorialEstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CU-03: usa la base H2 real y los mocks de Pedidos (100 a 105 CONFIRMADOS,
// 106 PENDIENTE, el resto no existe) y Notificaciones. Cada test se revierte al terminar
@DataJpaTest
@Import({EntregaService.class, PedidosClientMock.class, NotificacionesClientMock.class})
class EntregaServiceTest {

    @Autowired
    private EntregaService entregaService;

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private RepartidorRepository repartidorRepository;

    @Autowired
    private PedidosClientMock pedidosClient;

    // ---------- asignacion automatica ----------

    @Test
    void asignaUnRepartidorDisponible() {
        Repartidor repartidor = repartidorDisponible(1L, "1", null);

        EntregaResponse resultado = entregaService.asignarRepartidor(pedido(100L));

        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(resultado.getRepartidorId()).isEqualTo(repartidor.getId());
        assertThat(resultado.getFechaAsignacion()).isNotNull();
        // RN-07: el repartidor queda OCUPADO
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.OCUPADO);
        // RN-13: se genero un codigo de 4 digitos
        Entrega entrega = entregaRepository.findById(resultado.getId()).orElseThrow();
        assertThat(entrega.getCodigoConfirmacion()).matches("\\d{4}");
    }

    // RN-12: creacion + asignacion, con el actor SISTEMA
    @Test
    void registraLosDosCambiosEnElHistorial() {
        repartidorDisponible(1L, "1", null);

        EntregaResponse resultado = entregaService.asignarRepartidor(pedido(100L));

        List<HistorialEstadoEntrega> historial =
                entregaRepository.findById(resultado.getId()).orElseThrow().getHistorial();
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).getEstadoAnterior()).isNull();
        assertThat(historial.get(0).getEstadoNuevo()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(historial.get(1).getEstadoAnterior()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(historial.get(1).getEstadoNuevo()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(historial).allMatch(h -> EntregaService.ACTOR_SISTEMA.equals(h.getUsuarioActorId()));
    }

    // DM-07: elige al que hace mas tiempo no recibe una entrega (null = nunca recibio)
    @Test
    void eligeAlQueHaceMasTiempoQueNoRecibeUnaEntrega() {
        LocalDateTime hoy = LocalDateTime.of(2026, 11, 1, 12, 0);
        repartidorDisponible(1L, "1", hoy.minusHours(1));
        Repartidor masAntiguo = repartidorDisponible(2L, "2", hoy.minusDays(1));

        EntregaResponse resultado = entregaService.asignarRepartidor(pedido(100L));

        assertThat(resultado.getRepartidorId()).isEqualTo(masAntiguo.getId());
    }

    // RN-05: un repartidor INACTIVO no recibe entregas aunque figure DISPONIBLE
    @Test
    void noAsignaAUnRepartidorInactivo() {
        Repartidor inactivo = repartidorDisponible(1L, "1", null);
        inactivo.setEstado(EstadoRepartidor.INACTIVO);

        EntregaResponse resultado = entregaService.asignarRepartidor(pedido(100L));

        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
    }

    // ---------- sin repartidores (A4, DM-11) ----------

    @Test
    void sinRepartidoresLaEntregaQuedaPendiente() {
        EntregaResponse resultado = entregaService.asignarRepartidor(pedido(100L));

        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(resultado.getRepartidorId()).isNull();
        assertThat(entregaRepository.findById(resultado.getId()).orElseThrow().getHistorial())
                .hasSize(1);
    }

    @Test
    void alReintentarSeReutilizaLaEntregaPendiente() {
        EntregaResponse primero = entregaService.asignarRepartidor(pedido(100L));
        repartidorDisponible(1L, "1", null);

        EntregaResponse segundo = entregaService.asignarRepartidor(pedido(100L));

        assertThat(segundo.getId()).isEqualTo(primero.getId());
        assertThat(segundo.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entregaRepository.count()).isEqualTo(1);
    }

    // ---------- pedido (RN-11) ----------

    @Test
    void unPedidoInexistenteDa404() {
        assertError(pedido(999L), CodigoError.ORDER_NOT_FOUND);
    }

    @Test
    void unPedidoNoConfirmadoSeRechaza() {
        assertError(pedido(106L), CodigoError.ORDER_NOT_CONFIRMED);
    }

    @Test
    void unPedidoConEntregaAsignadaNoSeDuplica() {
        repartidorDisponible(1L, "1", null);
        entregaService.asignarRepartidor(pedido(100L));

        assertError(pedido(100L), CodigoError.DELIVERY_ALREADY_EXISTS);
    }

    // ---------- repartidor indicado por un administrador ----------

    @Test
    void asignaAlRepartidorIndicado() {
        repartidorDisponible(1L, "1", LocalDateTime.of(2026, 1, 1, 0, 0));
        Repartidor indicado = repartidorDisponible(2L, "2", LocalDateTime.now());
        AsignarEntregaRequest request = pedido(100L);
        request.setRepartidorId(indicado.getId());

        EntregaResponse resultado = entregaService.asignarRepartidor(request);

        assertThat(resultado.getRepartidorId()).isEqualTo(indicado.getId());
    }

    @Test
    void elRepartidorIndicadoNoDisponibleSeRechaza() {
        Repartidor indicado = repartidorDisponible(1L, "1", null);
        indicado.setDisponibilidad(Disponibilidad.NO_DISPONIBLE);
        AsignarEntregaRequest request = pedido(100L);
        request.setRepartidorId(indicado.getId());

        assertError(request, CodigoError.COURIER_NOT_AVAILABLE);
    }

    @Test
    void elRepartidorIndicadoInexistenteDa404() {
        AsignarEntregaRequest request = pedido(100L);
        request.setRepartidorId(999L);

        assertError(request, CodigoError.COURIER_NOT_FOUND);
    }

    // ---------- Notificaciones (DM-12) ----------

    @Test
    void siFallaElAvisoLaAsignacionNoSeRevierte() {
        // Service armado a mano con un cliente de Notificaciones que siempre falla
        EntregaService conAvisoQueFalla = new EntregaService(entregaRepository,
                repartidorRepository, pedidosClient,
                (entregaId, repartidorId, clienteId, codigo) -> {
                    throw new IllegalStateException("Notificaciones no responde");
                });
        repartidorDisponible(1L, "1", null);

        EntregaResponse resultado = conAvisoQueFalla.asignarRepartidor(pedido(100L));

        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
    }

    // ---------- helpers ----------

    // Verifica el codigo de error y que no se haya creado ninguna entrega nueva
    private void assertError(AsignarEntregaRequest request, CodigoError esperado) {
        long antes = entregaRepository.count();

        assertThatThrownBy(() -> entregaService.asignarRepartidor(request))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(esperado);
        assertThat(entregaRepository.count()).isEqualTo(antes);
    }

    private AsignarEntregaRequest pedido(Long pedidoId) {
        return new AsignarEntregaRequest(pedidoId, 200L, 300L,
                "Av. Corrientes 1234, CABA", "Lima 757, CABA", null);
    }

    private Repartidor repartidorDisponible(Long usuarioId, String dni,
                                            LocalDateTime ultimaAsignacion) {
        Repartidor repartidor = new Repartidor();
        repartidor.setUsuarioId(usuarioId);
        repartidor.setNombre("Juan");
        repartidor.setApellido("Perez");
        repartidor.setDni(dni);
        repartidor.setTelefono("1155554444");
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.DISPONIBLE);
        repartidor.setFechaAlta(LocalDateTime.now());
        repartidor.setUltimaAsignacion(ultimaAsignacion);
        return repartidorRepository.save(repartidor);
    }
}
