package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.client.UsuariosClientMock;
import ar.edu.uade.deliverygo.logistica.dto.CambiarDisponibilidadRequest;
import ar.edu.uade.deliverygo.logistica.dto.RegistrarRepartidorRequest;
import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.dto.VehiculoDTO;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.repository.EntregaRepository;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Reglas de negocio de CU-01 y CU-02. Usa la base H2 real y el mock de Usuarios
// (usuarios 1 a 5 elegibles, 6 inactivo, 7 con rol CLIENTE). Cada test se revierte al terminar.
// Los datos obligatorios (RN-01) se prueban aparte, en RequestsValidacionTest
@DataJpaTest
@Import({RepartidorService.class, UsuariosClientMock.class})
class RepartidorServiceTest {

    @Autowired
    private RepartidorService repartidorService;

    @Autowired
    private RepartidorRepository repartidorRepository;

    @Autowired
    private EntregaRepository entregaRepository;

    // ---------- CU-01: registrar repartidor ----------

    @Test
    void registraUnRepartidorActivoYNoDisponible() {
        RepartidorResponse resultado = repartidorService.registrarRepartidor(
                alta(1L, "30123456", TipoVehiculo.MOTO, "AB123CD"));

        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoRepartidor.ACTIVO);
        assertThat(resultado.getDisponibilidad()).isEqualTo(Disponibilidad.NO_DISPONIBLE);
        assertThat(resultado.getFechaAlta()).isNotNull();
        // la respuesta incluye dni y telefono
        assertThat(resultado.getDni()).isEqualTo("30123456");
        assertThat(resultado.getTelefono()).isEqualTo("1155554444");
        assertThat(resultado.getVehiculo().getTipo()).isEqualTo(TipoVehiculo.MOTO);
        assertThat(resultado.getVehiculo().getPatente()).isEqualTo("AB123CD");
        assertThat(repartidorRepository.count()).isEqualTo(1);
    }

    // RN-04

    @Test
    void laBicicletaNoNecesitaPatente() {
        RepartidorResponse resultado = repartidorService.registrarRepartidor(
                alta(1L, "30123456", TipoVehiculo.BICICLETA, null));

        assertThat(resultado.getVehiculo().getPatente()).isNull();
    }

    @Test
    void unaMotoSinPatenteSeRechaza() {
        assertErrorAlta(alta(1L, "30123456", TipoVehiculo.MOTO, null),
                CodigoError.VEHICLE_PLATE_REQUIRED);
    }

    @Test
    void unAutoConPatenteEnBlancoSeRechaza() {
        assertErrorAlta(alta(1L, "30123456", TipoVehiculo.AUTO, "   "),
                CodigoError.VEHICLE_PLATE_REQUIRED);
    }

    // RN-02

    @Test
    void noSePuedeRepetirElDni() {
        repartidorService.registrarRepartidor(alta(1L, "30123456", TipoVehiculo.BICICLETA, null));

        assertErrorAlta(alta(2L, "30123456", TipoVehiculo.BICICLETA, null),
                CodigoError.COURIER_DNI_DUPLICATED);
    }

    @Test
    void unUsuarioNoPuedeTenerDosRepartidores() {
        repartidorService.registrarRepartidor(alta(1L, "30123456", TipoVehiculo.BICICLETA, null));

        assertErrorAlta(alta(1L, "40987654", TipoVehiculo.BICICLETA, null),
                CodigoError.COURIER_ALREADY_REGISTERED);
    }

    // RN-03 (mock de Usuarios)

    @Test
    void unUsuarioInexistenteNoEsElegible() {
        assertErrorAlta(alta(99L, "30123456", TipoVehiculo.BICICLETA, null),
                CodigoError.USER_NOT_ELIGIBLE);
    }

    @Test
    void unUsuarioInactivoNoEsElegible() {
        assertErrorAlta(alta(6L, "30123456", TipoVehiculo.BICICLETA, null),
                CodigoError.USER_NOT_ELIGIBLE);
    }

    @Test
    void unUsuarioSinRolRepartidorNoEsElegible() {
        assertErrorAlta(alta(7L, "30123456", TipoVehiculo.BICICLETA, null),
                CodigoError.USER_NOT_ELIGIBLE);
    }

    // ---------- CU-02: cambiar disponibilidad ----------
    // El repartidor de prueba es el usuario 1; el usuario autenticado se pasa como parametro

    @Test
    void elTitularSePoneDisponible() {
        Long id = registrar(1L);

        RepartidorResponse resultado = repartidorService.cambiarDisponibilidad(
                id, new CambiarDisponibilidadRequest(Disponibilidad.DISPONIBLE), 1L);

        assertThat(resultado.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
        assertThat(repartidorRepository.findById(id).orElseThrow().getDisponibilidad())
                .isEqualTo(Disponibilidad.DISPONIBLE);
    }

    // A6: pedir el mismo valor no es un error y no cambia nada
    @Test
    void pedirElMismoValorNoCambiaNada() {
        Long id = registrar(1L);

        RepartidorResponse resultado = repartidorService.cambiarDisponibilidad(
                id, new CambiarDisponibilidadRequest(Disponibilidad.NO_DISPONIBLE), 1L);

        assertThat(resultado.getDisponibilidad()).isEqualTo(Disponibilidad.NO_DISPONIBLE);
    }

    // RN-07: OCUPADO solo lo maneja el sistema
    @Test
    void noSePuedePedirOcupado() {
        Long id = registrar(1L);

        assertErrorCambio(id, Disponibilidad.OCUPADO, 1L, CodigoError.VALIDATION_ERROR);
    }

    @Test
    void unRepartidorInexistenteDa404() {
        assertErrorCambio(999L, Disponibilidad.DISPONIBLE, 1L, CodigoError.COURIER_NOT_FOUND);
    }

    // RN-10
    @Test
    void otroUsuarioNoPuedeCambiarLaDisponibilidad() {
        Long id = registrar(1L);

        assertErrorCambio(id, Disponibilidad.DISPONIBLE, 2L, CodigoError.FORBIDDEN);
    }

    // RN-05
    @Test
    void unRepartidorInactivoNoPuedeCambiarla() {
        Long id = registrar(1L);
        repartidorRepository.findById(id).orElseThrow().setEstado(EstadoRepartidor.INACTIVO);

        assertErrorCambio(id, Disponibilidad.DISPONIBLE, 1L, CodigoError.COURIER_INACTIVE);
    }

    // RN-07: con una entrega ASIGNADA no puede cambiarla
    @Test
    void conUnaEntregaActivaNoPuedeCambiarla() {
        Long id = registrar(1L);
        repartidorService.cambiarDisponibilidad(
                id, new CambiarDisponibilidadRequest(Disponibilidad.DISPONIBLE), 1L);
        Repartidor repartidor = repartidorRepository.findById(id).orElseThrow();
        Entrega entrega = entregaPendiente();
        entrega.asignar(repartidor, "1234"); // deja al repartidor OCUPADO
        entregaRepository.save(entrega);

        assertErrorCambio(id, Disponibilidad.NO_DISPONIBLE, 1L,
                CodigoError.COURIER_HAS_ACTIVE_DELIVERY);
    }

    // ---------- helpers ----------

    // Verifica el codigo de error y que no se haya guardado un repartidor nuevo
    private void assertErrorAlta(RegistrarRepartidorRequest entrada, CodigoError esperado) {
        long antes = repartidorRepository.count();

        assertThatThrownBy(() -> repartidorService.registrarRepartidor(entrada))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(esperado);
        assertThat(repartidorRepository.count()).isEqualTo(antes);
    }

    private void assertErrorCambio(Long id, Disponibilidad pedida, Long usuarioId,
                                   CodigoError esperado) {
        CambiarDisponibilidadRequest entrada = new CambiarDisponibilidadRequest(pedida);

        assertThatThrownBy(() -> repartidorService.cambiarDisponibilidad(id, entrada, usuarioId))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(esperado);
    }

    private Long registrar(Long usuarioId) {
        return repartidorService.registrarRepartidor(
                alta(usuarioId, "30123456", TipoVehiculo.BICICLETA, null)).getId();
    }

    private RegistrarRepartidorRequest alta(Long usuarioId, String dni,
                                            TipoVehiculo tipo, String patente) {
        return new RegistrarRepartidorRequest(usuarioId, "Juan", "Perez", dni,
                "1155554444", new VehiculoDTO(tipo, patente, null));
    }

    private Entrega entregaPendiente() {
        Entrega entrega = new Entrega();
        entrega.setPedidoId(100L);
        entrega.setClienteId(200L);
        entrega.setComercioId(300L);
        entrega.setDireccionRetiro("Av. Corrientes 1234, CABA");
        entrega.setDireccionEntrega("Lima 757, CABA");
        entrega.setEstado(EstadoEntrega.PENDIENTE_ASIGNACION);
        entrega.setFechaCreacion(LocalDateTime.now());
        return entrega;
    }
}
