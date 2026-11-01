package ar.edu.uade.deliverygo.logistica.client;

// INT-01: Logistica -> Usuarios (GET /api/v1/users/{id})
// El service depende de esta interfaz y no de una implementacion concreta.
// Hoy la implementa UsuariosClientMock; cuando exista el modulo Usuarios
// se agrega una implementacion que haga la llamada HTTP real, sin tocar el service.
public interface UsuariosClient {

    // Devuelve el usuario, o null si no existe en el modulo Usuarios
    UsuarioExterno buscarUsuario(Long usuarioId);
}
