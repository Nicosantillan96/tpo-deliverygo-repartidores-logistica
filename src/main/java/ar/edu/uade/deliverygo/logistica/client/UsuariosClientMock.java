package ar.edu.uade.deliverygo.logistica.client;

import org.springframework.stereotype.Component;

import java.util.Map;

// Mock declarado de INT-01 mientras el modulo Usuarios no este disponible
// (seccion 11 de la primera entrega). Simula los usuarios con una lista fija:
// - 1 a 5: activos y con rol REPARTIDOR -> se pueden registrar como repartidores
// - 6: inactivo -> USER_NOT_ELIGIBLE
// - 7: rol CLIENTE -> USER_NOT_ELIGIBLE
// - cualquier otro: no existe -> USER_NOT_ELIGIBLE
@Component
public class UsuariosClientMock implements UsuariosClient {

    private static final Map<Long, UsuarioExterno> USUARIOS = Map.of(
            1L, new UsuarioExterno(1L, true, "REPARTIDOR"),
            2L, new UsuarioExterno(2L, true, "REPARTIDOR"),
            3L, new UsuarioExterno(3L, true, "REPARTIDOR"),
            4L, new UsuarioExterno(4L, true, "REPARTIDOR"),
            5L, new UsuarioExterno(5L, true, "REPARTIDOR"),
            6L, new UsuarioExterno(6L, false, "REPARTIDOR"),
            7L, new UsuarioExterno(7L, true, "CLIENTE")
    );

    @Override
    public UsuarioExterno buscarUsuario(Long usuarioId) {
        return USUARIOS.get(usuarioId);
    }
}
