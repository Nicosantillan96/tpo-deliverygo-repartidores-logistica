package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.UsuarioExterno;
import ar.edu.uade.deliverygo.logistica.client.UsuariosClient;
import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoDTO;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
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

    public RepartidorService(RepartidorRepository repartidorRepository,
                             EntregaRepository entregaRepository,
                             UsuariosClient usuariosClient) {
        this.repartidorRepository = repartidorRepository;
        this.entregaRepository = entregaRepository;
        this.usuariosClient = usuariosClient;
    }

    // SRV-01 / CU-01: registrar repartidor
    // RN-01 (datos obligatorios y formato) se valida en la frontera con Bean Validation
    // (@Valid en el controller, anotaciones en RegistrarRepartidorRequest y VehiculoDTO).
    // Aca se aplican las reglas que necesitan logica o consultas: RN-02, RN-03 y RN-04.
    @Transactional
    public RepartidorResponse registrarRepartidor(RegistrarRepartidorRequest dto) {

        // RN-04: MOTO y AUTO requieren patente; BICICLETA no
        validarVehiculo(dto.getVehiculo());

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
        return convertirResponse(guardado);
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
        return convertirResponse(repartidorRepository.save(repartidor));
    }

    private Repartidor buscarEntidadPorId(Long id) {
        return repartidorRepository.findById(id).orElseThrow(
                () -> new ReglaNegocioException(CodigoError.COURIER_NOT_FOUND,
                        "No existe el repartidor " + id));
    }

    // RN-04
    private void validarVehiculo(VehiculoDTO vehiculo) {
        boolean requierePatente = vehiculo.getTipo() == TipoVehiculo.MOTO
                || vehiculo.getTipo() == TipoVehiculo.AUTO;
        String patente = vehiculo.getPatente();
        if (requierePatente && (Objects.isNull(patente) || patente.isBlank())) {
            throw new ReglaNegocioException(CodigoError.VEHICLE_PLATE_REQUIRED,
                    "Los vehículos " + vehiculo.getTipo() + " requieren patente");
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

    // Entidad -> DTO-02. La entidad nunca se expone directamente en la API
    private RepartidorResponse convertirResponse(Repartidor repartidor) {
        Vehiculo activo = repartidor.vehiculoActivo();
        VehiculoDTO vehiculo = Objects.isNull(activo) ? null
                : new VehiculoDTO(activo.getTipo(), activo.getPatente(), activo.getDescripcion());

        return new RepartidorResponse(
                repartidor.getId(),
                repartidor.getUsuarioId(),
                repartidor.getNombre(),
                repartidor.getApellido(),
                repartidor.getDni(),
                repartidor.getTelefono(),
                repartidor.getEstado(),
                repartidor.getDisponibilidad(),
                vehiculo,
                repartidor.getFechaAlta());
    }
}
