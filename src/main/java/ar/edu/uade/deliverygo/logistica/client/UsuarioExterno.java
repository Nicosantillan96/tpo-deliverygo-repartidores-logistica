package ar.edu.uade.deliverygo.logistica.client;

// Datos que Logistica necesita del modulo Usuarios para RN-03 (INT-01).
// No es una entidad: el usuario pertenece a otro modulo y no se guarda aca (DM-01).
// Solo se incluyen los campos que usa CU-01; el contrato completo lo define Usuarios.
public class UsuarioExterno {

    private final Long id;
    private final boolean activo;
    private final String rol;

    public UsuarioExterno(Long id, boolean activo, String rol) {
        this.id = id;
        this.activo = activo;
        this.rol = rol;
    }

    //GETTERS
    public Long getId() {
        return id;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getRol() {
        return rol;
    }
}
