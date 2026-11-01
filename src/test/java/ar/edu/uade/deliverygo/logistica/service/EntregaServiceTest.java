package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.NotificacionesClient;
import ar.edu.uade.deliverygo.logistica.client.NotificacionesClientMock;
import ar.edu.uade.deliverygo.logistica.client.PedidosClientMock;
import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRetiroRequest;
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

// CU-03 y CU-04: usan la base H2 real y los mocks de Pedidos (100 a 105 CONFIRMADOS,
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
        repartidorDisponible(1L, "1", null);

        EntregaResponse resultado = serviceConAvisosQueFallan().asignarRepartidor(pedido(100L));

        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
    }

    // ---------- CU-04: registrar retiro ----------
    // La entrega de prueba queda asignada al repartidor del usuario 1

    @Test
    void elRepartidorAsignadoRegistraElRetiro() {
        Long entregaId = entregaAsignadaAlUsuario1();

        EntregaResponse resultado = entregaService.registrarRetiro(
                entregaId, new RegistrarRetiroRequest("Retirado en mostrador"), 1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.RETIRADA);
        assertThat(resultado.getFechaRetiro()).isNotNull();
    }

    // RN-12: el actor del retiro es el repartidor y se guarda la observacion
    @Test
    void elRetiroQuedaEnElHistorialConElRepartidorComoActor() {
        Long entregaId = entregaAsignadaAlUsuario1();

        entregaService.registrarRetiro(entregaId, new RegistrarRetiroRequest("Retirado"), 1L);

        List<HistorialEstadoEntrega> historial =
                entregaRepository.findById(entregaId).orElseThrow().getHistorial();
        HistorialEstadoEntrega ultimo = historial.get(historial.size() - 1);
        assertThat(historial).hasSize(3);
        assertThat(ultimo.getEstadoAnterior()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(ultimo.getEstadoNuevo()).isEqualTo(EstadoEntrega.RETIRADA);
        assertThat(ultimo.getUsuarioActorId()).isEqualTo(1L);
        assertThat(ultimo.getObservacion()).isEqualTo("Retirado");
    }

    @Test
    void elRetiroSinObservacionEsValido() {
        Long entregaId = entregaAsignadaAlUsuario1();

        EntregaResponse resultado = entregaService.registrarRetiro(
                entregaId, new RegistrarRetiroRequest(), 1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.RETIRADA);
    }

    @Test
    void retirarUnaEntregaInexistenteDa404() {
        assertErrorRetiro(999L, 1L, CodigoError.DELIVERY_NOT_FOUND);
    }

    // RN-10
    @Test
    void otroUsuarioNoPuedeRegistrarElRetiro() {
        Long entregaId = entregaAsignadaAlUsuario1();

        assertErrorRetiro(entregaId, 2L, CodigoError.COURIER_NOT_ASSIGNED);
    }

    // RN-10: una entrega PENDIENTE no tiene repartidor asignado
    @Test
    void noSePuedeRetirarUnaEntregaSinRepartidor() {
        Long entregaId = entregaService.asignarRepartidor(pedido(100L)).getId();

        assertErrorRetiro(entregaId, 1L, CodigoError.COURIER_NOT_ASSIGNED);
    }

    // RN-08: no se puede retirar dos veces
    @Test
    void noSePuedeRetirarDosVeces() {
        Long entregaId = entregaAsignadaAlUsuario1();
        entregaService.registrarRetiro(entregaId, new RegistrarRetiroRequest(), 1L);

        assertErrorRetiro(entregaId, 1L, CodigoError.DELIVERY_INVALID_STATE);
    }

    // DM-12
    @Test
    void siFallanLosAvisosElRetiroNoSeRevierte() {
        Long entregaId = entregaAsignadaAlUsuario1();

        EntregaResponse resultado = serviceConAvisosQueFallan().registrarRetiro(
                entregaId, new RegistrarRetiroRequest(), 1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEntrega.RETIRADA);
    }

    // ---------- helpers ----------

    private Long entregaAsignadaAlUsuario1() {
        repartidorDisponible(1L, "1", null);
        return entregaService.asignarRepartidor(pedido(100L)).getId();
    }

    private void assertErrorRetiro(Long entregaId, Long usuarioId, CodigoError esperado) {
        assertThatThrownBy(() -> entregaService.registrarRetiro(
                entregaId, new RegistrarRetiroRequest(), usuarioId))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(esperado);
    }

    // Service armado a mano con clientes cuyos avisos siempre fallan (DM-12).
    // buscarPedido sigue funcionando porque se hereda del mock
    private EntregaService serviceConAvisosQueFallan() {
        return new EntregaService(entregaRepository, repartidorRepository,
                new PedidosQueFallanAlInformar(), new NotificacionesQueFallan());
    }

    private static class PedidosQueFallanAlInformar extends PedidosClientMock {
        @Override
        public void informarEstadoEntrega(Long pedidoId, EstadoEntrega estado) {
            throw new IllegalStateException("Pedidos no responde");
        }
    }

    private static class NotificacionesQueFallan implements NotificacionesClient {
        @Override
        public void avisarAsignacion(Long entregaId, Long repartidorId, Long clienteId,
                                     String codigoConfirmacion) {
            throw new IllegalStateException("Notificaciones no responde");
        }

        @Override
        public void avisarRetiro(Long entregaId, Long clienteId) {
            throw new IllegalStateException("Notificaciones no responde");
        }
    }

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
