package ar.edu.uade.deliverygo.logistica.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// DTO-01: entrada de POST /api/v1/couriers (CU-01)
// RN-01: las anotaciones se validan en la frontera, cuando el controller recibe
// el request con @Valid. No tiene id, estado, disponibilidad ni fechaAlta:
// esos datos los define el sistema, asi que el cliente no puede mandarlos
public class RegistrarRepartidorRequest {

    @NotNull(message = "El usuarioId es obligatorio")
    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos, sin puntos")
    private String dni;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    // @Valid hace que tambien se validen las anotaciones de VehiculoDTO
    @NotNull(message = "El vehículo es obligatorio")
    @Valid
    private VehiculoDTO vehiculo;

    public RegistrarRepartidorRequest() {
    }

    public RegistrarRepartidorRequest(Long usuarioId, String nombre, String apellido,
                                      String dni, String telefono, VehiculoDTO vehiculo) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.telefono = telefono;
        this.vehiculo = vehiculo;
    }

    //GETTERS & SETTERS
    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
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

    public VehiculoDTO getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(VehiculoDTO vehiculo) {
        this.vehiculo = vehiculo;
    }
}
