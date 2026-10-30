package ar.edu.uade.deliverygo.logistica.dto;

// DTO-01: entrada de POST /api/v1/couriers (CU-01)
public class RegistrarRepartidorRequest {

    private Long usuarioId;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
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
