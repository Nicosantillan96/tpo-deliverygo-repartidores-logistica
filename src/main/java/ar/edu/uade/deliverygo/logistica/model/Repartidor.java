package ar.edu.uade.deliverygo.logistica.model;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "repartidores")

public class Repartidor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String apellido;
    @Column(nullable = false, unique = true)
    private String dni;
    @Column(nullable = false)
    private String telefono;


    //identificador del usuario para modulo externo
    @Column(name = "usuario_id",nullable = false,unique = true)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRepartidor estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disponibilidad disponibilidad;
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;
    @Column(name = "ultima_asignacion")
    private LocalDateTime ultimaAsignacion;
    @Version
    private Long version;
    @OneToMany(
            mappedBy = "repartidor",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Vehiculo> vehiculos = new ArrayList<>();

    // CU-10: zona en la que trabaja. Es opcional: sin zona puede recibir
    // entregas que no indican zona
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id")
    private Zona zona;

    public Repartidor() {
    }
    // Getters y setters

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public EstadoRepartidor getEstado() {
        return estado;
    }

    public void setEstado(EstadoRepartidor estado) {
        this.estado = estado;
    }

    public Disponibilidad getDisponibilidad() {
        return disponibilidad;
    }

    public void setDisponibilidad(Disponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public LocalDateTime getUltimaAsignacion() {
        return ultimaAsignacion;
    }

    public void setUltimaAsignacion(LocalDateTime ultimaAsignacion) {
        this.ultimaAsignacion = ultimaAsignacion;
    }

    public Long getVersion() {
        return version;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public void agregarVehiculo(Vehiculo vehiculo) {
        vehiculos.add(vehiculo);
        vehiculo.setRepartidor(this);
    }

    // Metodos de dominio

    // CU-02: el repartidor cambia su disponibilidad (RN-05, RN-07)
    public void cambiarDisponibilidad(Disponibilidad nueva) {
        // OCUPADO solo lo maneja el sistema
        if (nueva != Disponibilidad.DISPONIBLE && nueva != Disponibilidad.NO_DISPONIBLE) {
            throw new ReglaNegocioException(CodigoError.VALIDATION_ERROR,
                    "La disponibilidad solo puede cambiarse a DISPONIBLE o NO_DISPONIBLE");
        }
        if (estado != EstadoRepartidor.ACTIVO) {
            throw new ReglaNegocioException(CodigoError.COURIER_INACTIVE,
                    "El repartidor está inactivo");
        }
        if (disponibilidad == Disponibilidad.OCUPADO) {
            throw new ReglaNegocioException(CodigoError.COURIER_HAS_ACTIVE_DELIVERY,
                    "El repartidor tiene una entrega activa");
        }
        this.disponibilidad = nueva;
    }

    // CU-03: el sistema lo ocupa al asignarle una entrega (RN-05, RN-06, RN-07)
    public void ocupar() {
        if (estado != EstadoRepartidor.ACTIVO || disponibilidad != Disponibilidad.DISPONIBLE) {
            throw new ReglaNegocioException(CodigoError.COURIER_NOT_AVAILABLE,
                    "El repartidor no está activo y disponible");
        }
        this.disponibilidad = Disponibilidad.OCUPADO;
        this.ultimaAsignacion = LocalDateTime.now();
    }

    // CU-05: el sistema lo libera al completar la entrega (RN-07).
    // CU-06: tambien lo libera cuando rechaza la asignacion
    public void liberar() {
        // Si no esta OCUPADO es un error de programacion, no del usuario
        if (disponibilidad != Disponibilidad.OCUPADO) {
            throw new IllegalStateException("Solo se puede liberar un repartidor OCUPADO");
        }
        this.disponibilidad = Disponibilidad.DISPONIBLE;
    }

    public Vehiculo vehiculoActivo() {
        return vehiculos.stream()
                .filter(Vehiculo::isActivo)
                .findFirst()
                .orElse(null);
    }

    // CU-09: vehiculo del repartidor por id. Si no es suyo, para el es inexistente
    public Vehiculo buscarVehiculo(Long vehiculoId) {
        return vehiculos.stream()
                .filter(v -> Objects.equals(v.getId(), vehiculoId))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(CodigoError.VEHICLE_NOT_FOUND,
                        "El repartidor no tiene el vehículo " + vehiculoId));
    }

    // CU-09: cambia el vehiculo activo. Siempre queda uno solo activo (REL-01).
    // No se puede cambiar mientras tiene una entrega en curso (RN-07)
    public void activarVehiculo(Long vehiculoId) {
        Vehiculo elegido = buscarVehiculo(vehiculoId);
        if (disponibilidad == Disponibilidad.OCUPADO) {
            throw new ReglaNegocioException(CodigoError.COURIER_HAS_ACTIVE_DELIVERY,
                    "No se puede cambiar el vehículo durante una entrega");
        }
        vehiculos.forEach(v -> v.setActivo(v == elegido));
    }

    // CU-09: baja de un vehiculo. El activo no se puede quitar: primero hay que
    // activar otro, asi el repartidor nunca se queda sin vehiculo (RN-04).
    // orphanRemoval borra la fila al sacarlo de la lista
    public void quitarVehiculo(Long vehiculoId) {
        Vehiculo vehiculo = buscarVehiculo(vehiculoId);
        if (vehiculo.isActivo()) {
            throw new ReglaNegocioException(CodigoError.VEHICLE_IS_ACTIVE,
                    "No se puede quitar el vehículo activo; primero active otro");
        }
        vehiculos.remove(vehiculo);
    }

    // CU-10: zona en la que trabaja el repartidor
    public void asignarZona(Zona zona) {
        this.zona = Objects.requireNonNull(zona, "La zona es obligatoria");
    }

    public Zona getZona() {
        return zona;
    }
}
