package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.UsuarioExterno;
import ar.edu.uade.deliverygo.logistica.client.UsuariosClient;
import ar.edu.uade.deliverygo.logistica.dto.AgregarVehiculoRequest;
import ar.edu.uade.deliverygo.logistica.dto.AsignarZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoResponse;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.mapper.RepartidorMapper;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import ar.edu.uade.deliverygo.logistica.model.Zona;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import ar.edu.uade.deliverygo.logistica.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class RepartidorService {

    private static final String ROL_REPARTIDOR = "REPARTIDOR";

    // Estados en los que una entrega cuenta como activa (RN-07)
    private static final List<EstadoEntrega> ENTREGAS_ACTIVAS =
            List.of(EstadoEntrega.ASIGNADA, EstadoEntrega.RETIRADA, EstadoEntrega.EN_CAMINO);

    private final RepartidorRepository repartidorRepository;
    private final EntregaRepository entregaRepository;
    private final UsuariosClient usuariosClient;
    private final RepartidorMapper repartidorMapper;
    private final ZonaRepository zonaRepository;

    public RepartidorService(RepartidorRepository repartidorRepository,
                             EntregaRepository entregaRepository,
                             UsuariosClient usuariosClient,
                             RepartidorMapper repartidorMapper,
                             ZonaRepository zonaRepository) {
        this.repartidorRepository = repartidorRepository;
        this.zonaRepository = zonaRepository;
        this.entregaRepository = entregaRepository;
        this.usuariosClient = usuariosClient;
        this.repartidorMapper = repartidorMapper;
    }

    // SRV-01 / CU-01: registrar repartidor
    // RN-01 (datos obligatorios y formato) se valida en la frontera con Bean Validation
    // (@Valid en el controller, anotaciones en RegistrarRepartidorRequest y VehiculoDTO).
    // Aca se aplican las reglas que necesitan logica o consultas: RN-02, RN-03 y RN-04.
    @Transactional
    public RepartidorResponse registrarRepartidor(RegistrarRepartidorRequest dto) {

        // RN-04: MOTO y AUTO requieren patente; BICICLETA no
        validarVehiculo(dto.getVehiculo().getTipo(), dto.getVehiculo().getPatente());
        // La patente no puede estar en otro vehiculo (REP-11). Antes la rechazaba
        // la base con un 500; ahora se responde 409 VEHICLE_PLATE_DUPLICATED
        validarPatenteUnica(dto.getVehiculo().getPatente());

        // RN-02: DNI unico (REP-01)
        if (repartidorRepository.existsByDni(dto.getDni())) {
            throw new ReglaNegocioException(CodigoError.COURIER_DNI_DUPLICATED,
                    "Ya existe un repartidor con el DNI " + dto.getDni());
        }

        // RN-02: un usuario puede tener un solo repartidor (REP-02)
        if (repartidorRepository.existsByUsuarioId(dto.getUsuarioId())) {
            throw new ReglaNegocioException(CodigoError.COURIER_ALREADY_REGISTERED,
                    "El usuario " + dto.getUsuarioId() + " ya está registrado como repartidor");
        }

        // RN-03: el usuario existe, esta activo y tiene rol REPARTIDOR (INT-01)
        validarUsuarioElegible(dto.getUsuarioId());

        // Se crea el repartidor. estado, disponibilidad y fechaAlta los define
        // el sistema (el request no tiene esos campos)
        Repartidor repartidor = new Repartidor();
        repartidor.setUsuarioId(dto.getUsuarioId());
        repartidor.setNombre(dto.getNombre());
        repartidor.setApellido(dto.getApellido());
        repartidor.setDni(dto.getDni());
        repartidor.setTelefono(dto.getTelefono());
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.NO_DISPONIBLE);
        repartidor.setFechaAlta(LocalDateTime.now());

        // El vehiculo con el que se registra queda como vehiculo activo (REL-01)
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(dto.getVehiculo().getTipo());
        vehiculo.setPatente(dto.getVehiculo().getPatente());
        vehiculo.setDescripcion(dto.getVehiculo().getDescripcion());
        vehiculo.setActivo(true);
        repartidor.agregarVehiculo(vehiculo);

        // REP-08: el vehiculo se guarda en cascada con el repartidor
        Repartidor guardado = repartidorRepository.save(repartidor);
        return repartidorMapper.toResponse(guardado);
    }

    // SRV-02 / CU-02: el repartidor cambia su disponibilidad
    // usuarioId es el usuario autenticado que hace el pedido. Lo pasa el controller
    // a partir del mecanismo de autenticacion (RN-10), nunca sale del body
    @Transactional
    // RN-01 (disponibilidad obligatoria) se valida en la frontera con @Valid.
    public RepartidorResponse cambiarDisponibilidad(Long id, CambiarDisponibilidadRequest dto,
                                                    Long usuarioId) {

        // REP-03
        Repartidor repartidor = buscarEntidadPorId(id);

        // RN-10: solo el titular puede cambiar su disponibilidad
        if (!Objects.equals(repartidor.getUsuarioId(), usuarioId)) {
            throw new ReglaNegocioException(CodigoError.FORBIDDEN,
                    "Solo el titular puede cambiar la disponibilidad del repartidor");
        }

        // RN-07: no puede cambiarla si tiene una entrega activa (REP-05)
        if (entregaRepository.existsByRepartidorIdAndEstadoIn(id, ENTREGAS_ACTIVAS)) {
            throw new ReglaNegocioException(CodigoError.COURIER_HAS_ACTIVE_DELIVERY,
                    "El repartidor tiene una entrega activa");
        }

        // RN-05 (ACTIVO) y RN-07 (no pedir OCUPADO, no estar OCUPADO) en la entidad.
        // Si pide el mismo valor que ya tiene, no cambia nada (A6, idempotente)
        repartidor.cambiarDisponibilidad(dto.getDisponibilidad());

        // REP-08
        return repartidorMapper.toResponse(repartidorRepository.save(repartidor));
    }

    // SRV-11 / CU-08: datos del repartidor. readOnly: solo lee; la transaccion hace
    // falta para leer los vehiculos (LAZY) porque open-in-view esta desactivado
    @Transactional(readOnly = true)
    public RepartidorResponse buscarPorId(Long id) {
        return repartidorMapper.toResponse(buscarEntidadPorId(id));
    }

    // SRV-12 / CU-09: vehiculos del repartidor
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarVehiculos(Long id) {
        return repartidorMapper.toVehiculosResponse(buscarEntidadPorId(id));
    }

    // SRV-13 / CU-09: el titular agrega un vehiculo. Queda inactivo hasta que lo active.
    // Devuelve todos sus vehiculos, asi se ve el id del nuevo
    @Transactional
    public List<VehiculoResponse> agregarVehiculo(Long id, AgregarVehiculoRequest request,
                                                  Long usuarioId) {
        Repartidor repartidor = buscarEntidadPorId(id);
        // RN-10: solo el titular administra sus vehiculos
        validarTitular(repartidor, usuarioId);
        // RN-04 y patente unica (REP-11), igual que en CU-01
        validarVehiculo(request.getTipo(), request.getPatente());
        validarPatenteUnica(request.getPatente());

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(request.getTipo());
        vehiculo.setPatente(request.getPatente());
        vehiculo.setDescripcion(request.getDescripcion());
        vehiculo.setActivo(false);
        repartidor.agregarVehiculo(vehiculo);

        // REP-08: el vehiculo se guarda en cascada con el repartidor
        return repartidorMapper.toVehiculosResponse(repartidorRepository.save(repartidor));
    }

    // SRV-14 / CU-09: el titular elige su vehiculo activo
    @Transactional
    public List<VehiculoResponse> activarVehiculo(Long id, Long vehiculoId, Long usuarioId) {
        Repartidor repartidor = buscarEntidadPorId(id);
        validarTitular(repartidor, usuarioId);
        // La entidad valida que el vehiculo sea suyo y que no este en una entrega (RN-07)
        repartidor.activarVehiculo(vehiculoId);
        return repartidorMapper.toVehiculosResponse(repartidorRepository.save(repartidor));
    }

    // SRV-15 / CU-09: el titular da de baja un vehiculo que no es el activo
    @Transactional
    public void quitarVehiculo(Long id, Long vehiculoId, Long usuarioId) {
        Repartidor repartidor = buscarEntidadPorId(id);
        validarTitular(repartidor, usuarioId);
        // La entidad valida que sea suyo y que no sea el activo; orphanRemoval borra la fila
        repartidor.quitarVehiculo(vehiculoId);
        repartidorRepository.save(repartidor);
    }

    // SRV-16 / CU-10: el titular elige la zona en la que trabaja
    @Transactional
    public RepartidorResponse asignarZona(Long id, AsignarZonaRequest request, Long usuarioId) {
        Repartidor repartidor = buscarEntidadPorId(id);
        validarTitular(repartidor, usuarioId);
        Zona zona = zonaRepository.findById(request.getZonaId()).orElseThrow(
                () -> new ReglaNegocioException(CodigoError.ZONE_NOT_FOUND,
                        "No existe la zona " + request.getZonaId()));
        repartidor.asignarZona(zona);
        return repartidorMapper.toResponse(repartidorRepository.save(repartidor));
    }

    private Repartidor buscarEntidadPorId(Long id) {
        return repartidorRepository.findById(id).orElseThrow(
                () -> new ReglaNegocioException(CodigoError.COURIER_NOT_FOUND,
                        "No existe el repartidor " + id));
    }

    // RN-10: el usuario autenticado tiene que ser el titular del repartidor
    private void validarTitular(Repartidor repartidor, Long usuarioId) {
        if (!Objects.equals(repartidor.getUsuarioId(), usuarioId)) {
            throw new ReglaNegocioException(CodigoError.FORBIDDEN,
                    "Solo el titular puede modificar los datos del repartidor");
        }
    }

    // RN-04
    private void validarVehiculo(TipoVehiculo tipo, String patente) {
        boolean requierePatente = tipo == TipoVehiculo.MOTO || tipo == TipoVehiculo.AUTO;
        if (requierePatente && (Objects.isNull(patente) || patente.isBlank())) {
            throw new ReglaNegocioException(CodigoError.VEHICLE_PLATE_REQUIRED,
                    "Los vehículos " + tipo + " requieren patente");
        }
    }

    // REP-11: una patente no puede estar en dos vehiculos (las bicicletas no tienen)
    private void validarPatenteUnica(String patente) {
        if (Objects.nonNull(patente) && !patente.isBlank()
                && repartidorRepository.existsByVehiculosPatente(patente)) {
            throw new ReglaNegocioException(CodigoError.VEHICLE_PLATE_DUPLICATED,
                    "Ya existe un vehículo con la patente " + patente);
        }
    }

    // RN-03
    private void validarUsuarioElegible(Long usuarioId) {
        UsuarioExterno usuario = usuariosClient.buscarUsuario(usuarioId);
        if (Objects.isNull(usuario)
                || !usuario.isActivo()
                || !ROL_REPARTIDOR.equals(usuario.getRol())) {
            throw new ReglaNegocioException(CodigoError.USER_NOT_ELIGIBLE,
                    "El usuario " + usuarioId + " no existe, está inactivo o no tiene rol REPARTIDOR");
        }
    }
}
