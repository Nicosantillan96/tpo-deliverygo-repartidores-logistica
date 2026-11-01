package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.NotificacionesClient;
import ar.edu.uade.deliverygo.logistica.client.PedidoExterno;
import ar.edu.uade.deliverygo.logistica.client.PedidosClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class EntregaService {

    private static final Logger log = LoggerFactory.getLogger(EntregaService.class);

    // Actor que se registra en el historial cuando el cambio lo hace el sistema
    // o el modulo Pedidos, y no una persona (RN-12)
    public static final Long ACTOR_SISTEMA = 0L;

    private static final String PEDIDO_CONFIRMADO = "CONFIRMADO";

    // Estados en los que una entrega cuenta como activa (RN-07)
    private static final List<EstadoEntrega> ENTREGAS_ACTIVAS =
            List.of(EstadoEntrega.ASIGNADA, EstadoEntrega.RETIRADA, EstadoEntrega.EN_CAMINO);

    // SecureRandom en lugar de Random: el codigo protege la entrega y no debe ser predecible
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EntregaRepository entregaRepository;
    private final RepartidorRepository repartidorRepository;
    private final PedidosClient pedidosClient;
    private final NotificacionesClient notificacionesClient;

    public EntregaService(EntregaRepository entregaRepository,
                          RepartidorRepository repartidorRepository,
                          PedidosClient pedidosClient,
                          NotificacionesClient notificacionesClient) {
        this.entregaRepository = entregaRepository;
        this.repartidorRepository = repartidorRepository;
        this.pedidosClient = pedidosClient;
        this.notificacionesClient = notificacionesClient;
    }

    // SRV-03 / CU-03: asignar repartidor a una entrega
    // RN-01 (datos obligatorios) se valida en la frontera con @Valid.
    // Devuelve la entrega ASIGNADA (el controller responde 201) o, si no hay
    // repartidores disponibles, PENDIENTE_ASIGNACION (el controller responde 202, DM-11).
    // Concurrencia (DM-09, simplificado): si dos asignaciones toman al mismo repartidor,
    // @Version en Repartidor hace fallar a la segunda al guardar y se responde 409;
    // Pedidos puede volver a pedir la asignacion. No se reintenta automaticamente.
    @Transactional
    public EntregaResponse asignarRepartidor(AsignarEntregaRequest request) {

        // RN-11: el pedido existe y esta CONFIRMADO (INT-03)
        validarPedidoConfirmado(request.getPedidoId());

        // RN-11: un pedido tiene como maximo una entrega no cancelada (REP-06).
        // Si quedo una PENDIENTE_ASIGNACION de un intento anterior, se reutiliza (DM-11)
        Entrega entrega = obtenerOCrearEntrega(request);

        // RN-05, RN-06, DM-07: repartidor indicado o el que hace mas tiempo no recibe una entrega
        Optional<Repartidor> elegido = elegirRepartidor(request.getRepartidorId());
        if (elegido.isEmpty()) {
            // A4: no hay repartidores disponibles; la entrega queda pendiente
            return convertirResponse(entregaRepository.save(entrega));
        }
        Repartidor repartidor = elegido.get();

        // RN-08, RN-13, RN-07: la entidad valida la transicion, guarda el codigo
        // y deja al repartidor OCUPADO
        EstadoEntrega anterior = entrega.getEstado();
        entrega.asignar(repartidor, generarCodigoConfirmacion());
        registrarCambioEstado(entrega, anterior, ACTOR_SISTEMA, null);

        // REP-09 (historial en cascada) y REP-08 (con @Version)
        repartidorRepository.save(repartidor);
        Entrega guardada = entregaRepository.save(entrega);

        // INT-05: avisos no bloqueantes (DM-12)
        avisarSinRevertir("asignacion de la entrega " + guardada.getId(),
                () -> notificacionesClient.avisarAsignacion(guardada.getId(),
                        repartidor.getId(), guardada.getClienteId(),
                        guardada.getCodigoConfirmacion()));

        return convertirResponse(guardada);
    }

    // SRV-04 / CU-04: registrar retiro del pedido
    // usuarioId es el usuario autenticado; lo pasa el controller (RN-10).
    // La observacion es opcional; su largo se valida en la frontera con @Valid
    @Transactional
    public EntregaResponse registrarRetiro(Long entregaId, RegistrarRetiroRequest request,
                                           Long usuarioId) {

        // REP-07
        Entrega entrega = buscarEntidadPorId(entregaId);

        // RN-10: solo el repartidor asignado registra el retiro
        validarRepartidorAsignado(entrega, usuarioId);

        // RN-08: ASIGNADA -> RETIRADA (la entidad rechaza cualquier otro estado
        // con DELIVERY_INVALID_STATE) y registra la fecha de retiro
        EstadoEntrega anterior = entrega.getEstado();
        entrega.registrarRetiro();

        // RN-12: el actor es el repartidor que hizo el retiro
        registrarCambioEstado(entrega, anterior, usuarioId, request.getObservacion());

        // REP-09
        Entrega guardada = entregaRepository.save(entrega);

        // INT-04 e INT-05: avisos no bloqueantes (DM-12)
        avisarSinRevertir("retiro de la entrega " + guardada.getId() + " a Pedidos",
                () -> pedidosClient.informarEstadoEntrega(guardada.getPedidoId(),
                        guardada.getEstado()));
        avisarSinRevertir("retiro de la entrega " + guardada.getId() + " a Notificaciones",
                () -> notificacionesClient.avisarRetiro(guardada.getId(),
                        guardada.getClienteId()));

        return convertirResponse(guardada);
    }

    private Entrega buscarEntidadPorId(Long entregaId) {
        return entregaRepository.findById(entregaId).orElseThrow(
                () -> new ReglaNegocioException(CodigoError.DELIVERY_NOT_FOUND,
                        "No existe la entrega " + entregaId));
    }

    // RN-10: el usuario autenticado tiene que ser el repartidor asignado a la entrega.
    // Si la entrega todavia no tiene repartidor, nadie esta asignado
    private void validarRepartidorAsignado(Entrega entrega, Long usuarioId) {
        Repartidor asignado = entrega.getRepartidor();
        if (Objects.isNull(asignado) || !Objects.equals(asignado.getUsuarioId(), usuarioId)) {
            throw new ReglaNegocioException(CodigoError.COURIER_NOT_ASSIGNED,
                    "El usuario no es el repartidor asignado a la entrega " + entrega.getId());
        }
    }

    // RN-11 / INT-03
    private void validarPedidoConfirmado(Long pedidoId) {
        PedidoExterno pedido = pedidosClient.buscarPedido(pedidoId);
        if (Objects.isNull(pedido)) {
            throw new ReglaNegocioException(CodigoError.ORDER_NOT_FOUND,
                    "No existe el pedido " + pedidoId);
        }
        if (!PEDIDO_CONFIRMADO.equals(pedido.getEstado())) {
            throw new ReglaNegocioException(CodigoError.ORDER_NOT_CONFIRMED,
                    "El pedido " + pedidoId + " no está confirmado");
        }
    }

    // RN-11 / DM-11
    private Entrega obtenerOCrearEntrega(AsignarEntregaRequest request) {
        Optional<Entrega> existente = entregaRepository
                .findByPedidoIdAndEstadoNot(request.getPedidoId(), EstadoEntrega.CANCELADA);

        if (existente.isPresent()) {
            Entrega entrega = existente.get();
            if (entrega.getEstado() != EstadoEntrega.PENDIENTE_ASIGNACION) {
                throw new ReglaNegocioException(CodigoError.DELIVERY_ALREADY_EXISTS,
                        "El pedido " + request.getPedidoId() + " ya tiene una entrega asignada");
            }
            return entrega;
        }

        Entrega nueva = new Entrega();
        nueva.setPedidoId(request.getPedidoId());
        nueva.setClienteId(request.getClienteId());
        nueva.setComercioId(request.getComercioId());
        nueva.setDireccionRetiro(request.getDireccionRetiro());
        nueva.setDireccionEntrega(request.getDireccionEntrega());
        nueva.setEstado(EstadoEntrega.PENDIENTE_ASIGNACION);
        nueva.setFechaCreacion(LocalDateTime.now());
        // RN-12: toda entrega nace con su primer registro de historial (REL-03)
        registrarCambioEstado(nueva, null, ACTOR_SISTEMA, null);
        return nueva;
    }

    // RN-05, RN-06, RN-07, DM-07
    private Optional<Repartidor> elegirRepartidor(Long repartidorId) {
        if (Objects.isNull(repartidorId)) {
            // REP-04: ACTIVO y DISPONIBLE, el que hace mas tiempo no recibe una entrega
            return repartidorRepository.findFirstByEstadoAndDisponibilidadOrderByUltimaAsignacionAsc(
                    EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE);
        }

        // Repartidor indicado por un administrador (REP-03)
        Repartidor repartidor = repartidorRepository.findById(repartidorId).orElseThrow(
                () -> new ReglaNegocioException(CodigoError.COURIER_NOT_FOUND,
                        "No existe el repartidor " + repartidorId));

        // RN-07: no puede tener otra entrega activa (REP-05).
        // ACTIVO y DISPONIBLE (RN-05, RN-06) lo controla Repartidor.ocupar()
        if (entregaRepository.existsByRepartidorIdAndEstadoIn(repartidorId, ENTREGAS_ACTIVAS)) {
            throw new ReglaNegocioException(CodigoError.COURIER_NOT_AVAILABLE,
                    "El repartidor " + repartidorId + " ya tiene una entrega activa");
        }
        return Optional.of(repartidor);
    }

    // RN-13 / DM-10: codigo de 4 digitos, de 0000 a 9999
    private String generarCodigoConfirmacion() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }

    // SRV-06: registra en el historial el cambio que la entidad ya aplico (RN-12).
    // La transicion la valida la entidad (RN-08); aca solo se deja constancia
    private void registrarCambioEstado(Entrega entrega, EstadoEntrega anterior,
                                       Long actorId, String observacion) {
        HistorialEstadoEntrega registro = new HistorialEstadoEntrega();
        registro.setEstadoAnterior(anterior);
        registro.setEstadoNuevo(entrega.getEstado());
        registro.setFechaHora(LocalDateTime.now());
        registro.setUsuarioActorId(actorId);
        registro.setObservacion(observacion);
        entrega.agregarHistorial(registro);
    }

    // DM-12: los avisos a otros modulos (Pedidos, Notificaciones) no revierten la
    // operacion si fallan; el fallo queda registrado en el log.
    // aviso es una lambda con la llamada al cliente, por ejemplo
    // () -> notificacionesClient.avisarRetiro(...)
    private void avisarSinRevertir(String descripcion, Runnable aviso) {
        try {
            aviso.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar {}: {}", descripcion, e.getMessage());
        }
    }

    // Entidad -> DTO-05. Nunca incluye el codigo de confirmacion (RN-13)
    private EntregaResponse convertirResponse(Entrega entrega) {
        Long repartidorId = Objects.isNull(entrega.getRepartidor()) ? null
                : entrega.getRepartidor().getId();
        return new EntregaResponse(
                entrega.getId(),
                entrega.getPedidoId(),
                repartidorId,
                entrega.getEstado(),
                entrega.getFechaCreacion(),
                entrega.getFechaAsignacion(),
                entrega.getFechaRetiro(),
                entrega.getFechaEntrega());
    }
}
