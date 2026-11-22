package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.AnaliticaClient;
import ar.edu.uade.deliverygo.logistica.client.NotificacionesClient;
import ar.edu.uade.deliverygo.logistica.client.PedidoExterno;
import ar.edu.uade.deliverygo.logistica.client.PedidosClient;
import ar.edu.uade.deliverygo.logistica.dto.AsignarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.ConfirmarEntregaRequest;
import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.RechazarAsignacionRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRetiroRequest;
import ar.edu.uade.deliverygo.logistica.dto.SeguimientoEntregaResponse;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.mapper.EntregaMapper;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.HistorialEstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import ar.edu.uade.deliverygo.logistica.repository.ZonaRepository;
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
    private final AnaliticaClient analiticaClient;
    private final EntregaMapper entregaMapper;
    private final ZonaRepository zonaRepository;

    public EntregaService(EntregaRepository entregaRepository,
                          RepartidorRepository repartidorRepository,
                          PedidosClient pedidosClient,
                          NotificacionesClient notificacionesClient,
                          AnaliticaClient analiticaClient,
                          EntregaMapper entregaMapper,
                          ZonaRepository zonaRepository) {
        this.entregaRepository = entregaRepository;
        this.repartidorRepository = repartidorRepository;
        this.zonaRepository = zonaRepository;
        this.pedidosClient = pedidosClient;
        this.notificacionesClient = notificacionesClient;
        this.analiticaClient = analiticaClient;
        this.entregaMapper = entregaMapper;
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

        // CU-10: si la entrega indica zona, la zona tiene que existir
        if (Objects.nonNull(request.getZonaId()) && !zonaRepository.existsById(request.getZonaId())) {
            throw new ReglaNegocioException(CodigoError.ZONE_NOT_FOUND,
                    "No existe la zona " + request.getZonaId());
        }

        // RN-11: un pedido tiene como maximo una entrega no cancelada (REP-06).
        // Si quedo una PENDIENTE_ASIGNACION de un intento anterior, se reutiliza (DM-11)
        Entrega entrega = obtenerOCrearEntrega(request);

        // RN-05, RN-06, DM-07: repartidor indicado o el que hace mas tiempo no recibe una entrega
        // (CU-10: de la zona de la entrega, si tiene)
        Optional<Repartidor> elegido = elegirRepartidor(request.getRepartidorId(), entrega.getZonaId());
        if (elegido.isEmpty()) {
            // A4: no hay repartidores disponibles; la entrega queda pendiente
            return entregaMapper.toResponse(entregaRepository.save(entrega));
        }

        asignarA(entrega, elegido.get());
        Entrega guardada = entregaRepository.save(entrega);
        avisarAsignacion(guardada);
        return entregaMapper.toResponse(guardada);
    }

    // SRV-07 / CU-06: el repartidor asignado acepta la asignacion.
    // La entrega sigue ASIGNADA; la aceptacion queda en el historial (RN-12)
    @Transactional
    public EntregaResponse aceptarAsignacion(Long entregaId, Long usuarioId) {
        Entrega entrega = buscarEntidadPorId(entregaId);
        // RN-10: solo el repartidor asignado puede aceptar
        validarRepartidorAsignado(entrega, usuarioId);

        // RN-08: solo mientras este ASIGNADA (despues del retiro ya no tiene sentido)
        EstadoEntrega anterior = entrega.getEstado();
        entrega.aceptar();
        registrarCambioEstado(entrega, anterior, usuarioId, "Asignación aceptada");

        return entregaMapper.toResponse(entregaRepository.save(entrega));
    }

    // SRV-08 / CU-06: el repartidor asignado rechaza la asignacion.
    // ASIGNADA -> PENDIENTE_ASIGNACION y se reasigna enseguida a otro repartidor
    // (mismo criterio que CU-03). Si no hay otro, la entrega queda pendiente, como en CU-03 A4
    @Transactional
    public EntregaResponse rechazarAsignacion(Long entregaId, RechazarAsignacionRequest request,
                                              Long usuarioId) {
        Entrega entrega = buscarEntidadPorId(entregaId);
        // RN-10: solo el repartidor asignado puede rechazar
        validarRepartidorAsignado(entrega, usuarioId);
        Repartidor quienRechaza = entrega.getRepartidor();

        // Se busca el reemplazo ANTES de liberar a quien rechaza: como todavia esta
        // OCUPADO, la consulta no lo devuelve y no se le vuelve a asignar la misma entrega
        Optional<Repartidor> reemplazo = elegirAutomatico(entrega.getZonaId());

        // RN-08 y RN-07: la entidad valida que este ASIGNADA y libera al repartidor
        EstadoEntrega anterior = entrega.getEstado();
        entrega.rechazar();
        registrarCambioEstado(entrega, anterior, usuarioId, request.getMotivo());
        repartidorRepository.save(quienRechaza);

        // Reasignacion: codigo nuevo y aviso, igual que CU-03
        reemplazo.ifPresent(repartidor -> asignarA(entrega, repartidor));
        Entrega guardada = entregaRepository.save(entrega);
        if (reemplazo.isPresent()) {
            avisarAsignacion(guardada);
        }
        return entregaMapper.toResponse(guardada);
    }

    // SRV-09 / CU-07: el repartidor asignado informa que va en camino
    @Transactional
    public EntregaResponse informarEnCamino(Long entregaId, Long usuarioId) {
        Entrega entrega = buscarEntidadPorId(entregaId);
        // RN-10: solo el repartidor asignado
        validarRepartidorAsignado(entrega, usuarioId);

        // RN-08: RETIRADA -> EN_CAMINO
        EstadoEntrega anterior = entrega.getEstado();
        entrega.informarEnCamino();
        // RN-12: el actor es el repartidor
        registrarCambioEstado(entrega, anterior, usuarioId, null);
        Entrega guardada = entregaRepository.save(entrega);

        // INT-05: aviso no bloqueante (DM-12). A Pedidos no se le informa:
        // segun la maquina de estados (seccion 7), este paso no cambia el pedido
        avisarSinRevertir("entrega " + guardada.getId() + " en camino a Notificaciones",
                () -> notificacionesClient.avisarEnCamino(guardada.getId(),
                        guardada.getClienteId()));
        return entregaMapper.toResponse(guardada);
    }

    // SRV-10 / CU-08: seguimiento de la entrega con su historial.
    // readOnly: solo lee. La transaccion hace falta para leer el historial (LAZY)
    // porque open-in-view esta desactivado
    @Transactional(readOnly = true)
    public SeguimientoEntregaResponse consultarSeguimiento(Long entregaId) {
        return entregaMapper.toSeguimientoResponse(buscarEntidadPorId(entregaId));
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

        return entregaMapper.toResponse(guardada);
    }

    // SRV-05 / CU-05: confirmar entrega del pedido
    // usuarioId es el usuario autenticado; lo pasa el controller (RN-10).
    // Que el codigo venga se valida en la frontera con @Valid
    @Transactional
    public EntregaResponse confirmarEntrega(Long entregaId, ConfirmarEntregaRequest request,
                                            Long usuarioId) {

        // REP-07
        Entrega entrega = buscarEntidadPorId(entregaId);

        // RN-10: solo el repartidor asignado confirma la entrega
        validarRepartidorAsignado(entrega, usuarioId);

        // La entidad aplica, en este orden:
        // RN-09: si sigue ASIGNADA (no se retiro) -> DELIVERY_NOT_PICKED_UP
        // RN-08: si ya esta COMPLETADA o CANCELADA -> DELIVERY_INVALID_STATE
        // RN-13: si el codigo no coincide -> INVALID_CONFIRMATION_CODE (la entrega no cambia)
        // y si todo esta bien la pasa a COMPLETADA y libera al repartidor (RN-07)
        EstadoEntrega anterior = entrega.getEstado();
        entrega.completar(request.getCodigoConfirmacion());

        // RN-12: el actor es el repartidor que confirmo la entrega
        registrarCambioEstado(entrega, anterior, usuarioId, request.getObservacion());

        // REP-08 (repartidor DISPONIBLE, con @Version) y REP-09
        Repartidor repartidor = entrega.getRepartidor();
        repartidorRepository.save(repartidor);
        Entrega guardada = entregaRepository.save(entrega);

        // INT-04, INT-05 e INT-06: avisos no bloqueantes (DM-12)
        avisarSinRevertir("entrega " + guardada.getId() + " completada a Pedidos",
                () -> pedidosClient.informarEstadoEntrega(guardada.getPedidoId(),
                        guardada.getEstado()));
        avisarSinRevertir("entrega " + guardada.getId() + " completada a Notificaciones",
                () -> notificacionesClient.avisarEntrega(guardada.getId(),
                        guardada.getClienteId()));
        avisarSinRevertir("entrega " + guardada.getId() + " completada a Analitica",
                () -> analiticaClient.registrarEntregaCompletada(guardada.getId(),
                        repartidor.getId(), guardada.getFechaCreacion(),
                        guardada.getFechaAsignacion(), guardada.getFechaRetiro(),
                        guardada.getFechaEntrega()));

        return entregaMapper.toResponse(guardada);
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
        nueva.setZonaId(request.getZonaId());
        nueva.setEstado(EstadoEntrega.PENDIENTE_ASIGNACION);
        nueva.setFechaCreacion(LocalDateTime.now());
        // RN-12: toda entrega nace con su primer registro de historial (REL-03)
        registrarCambioEstado(nueva, null, ACTOR_SISTEMA, null);
        return nueva;
    }

    // RN-08, RN-13, RN-07: la entidad valida la transicion, guarda el codigo
    // y deja al repartidor OCUPADO. Lo usan CU-03 y la reasignacion de CU-06
    private void asignarA(Entrega entrega, Repartidor repartidor) {
        EstadoEntrega anterior = entrega.getEstado();
        entrega.asignar(repartidor, generarCodigoConfirmacion());
        registrarCambioEstado(entrega, anterior, ACTOR_SISTEMA, null);
        // REP-08 (con @Version)
        repartidorRepository.save(repartidor);
    }

    // INT-05: aviso no bloqueante (DM-12). El codigo va al cliente, nunca en la respuesta
    private void avisarAsignacion(Entrega guardada) {
        avisarSinRevertir("asignacion de la entrega " + guardada.getId(),
                () -> notificacionesClient.avisarAsignacion(guardada.getId(),
                        guardada.getRepartidor().getId(), guardada.getClienteId(),
                        guardada.getCodigoConfirmacion()));
    }

    // RN-05, RN-06, RN-07, DM-07
    private Optional<Repartidor> elegirRepartidor(Long repartidorId, Long zonaId) {
        if (Objects.isNull(repartidorId)) {
            return elegirAutomatico(zonaId);
        }

        // Repartidor indicado por un administrador (REP-03). No se filtra por zona:
        // si el administrador lo elige, se respeta su decision
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

    // ACTIVO y DISPONIBLE, el que hace mas tiempo no recibe una entrega:
    // REP-04 entre todos, o REP-10 solo en la zona de la entrega (CU-10)
    private Optional<Repartidor> elegirAutomatico(Long zonaId) {
        if (Objects.isNull(zonaId)) {
            return repartidorRepository.findFirstByEstadoAndDisponibilidadOrderByUltimaAsignacionAsc(
                    EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE);
        }
        return repartidorRepository.findFirstByEstadoAndDisponibilidadAndZonaIdOrderByUltimaAsignacionAsc(
                EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, zonaId);
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
}
