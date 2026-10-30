package ar.edu.uade.deliverygo.logistica.repository;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RepartidorRepositoryTest {

    @Autowired
    private RepartidorRepository repartidorRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardarRepartidorGuardaSusVehiculosEnCascada() {
        Repartidor repartidor = nuevoRepartidor();
        repartidor.agregarVehiculo(nuevoVehiculo(TipoVehiculo.MOTO, "AB123CD"));
        // La bicicleta no tiene patente (RN-04)
        repartidor.agregarVehiculo(nuevoVehiculo(TipoVehiculo.BICICLETA, null));

        Long id = repartidorRepository.save(repartidor).getId();
        entityManager.flush();
        entityManager.clear();

        Repartidor leido = repartidorRepository.findById(id).orElseThrow();

        assertThat(leido.getVehiculos()).hasSize(2);
        assertThat(leido.getVersion()).isNotNull();
        assertThat(vehiculoRepository.count()).isEqualTo(2);
    }

    @Test
    void quitarUnVehiculoDeLaListaLoBorraPorOrphanRemoval() {
        Repartidor repartidor = nuevoRepartidor();
        repartidor.agregarVehiculo(nuevoVehiculo(TipoVehiculo.MOTO, "AB123CD"));
        repartidor.agregarVehiculo(nuevoVehiculo(TipoVehiculo.AUTO, "AC456EF"));

        Long id = repartidorRepository.save(repartidor).getId();
        entityManager.flush();
        entityManager.clear();

        Repartidor leido = repartidorRepository.findById(id).orElseThrow();
        leido.getVehiculos().remove(0);
        entityManager.flush();
        entityManager.clear();

        assertThat(vehiculoRepository.count()).isEqualTo(1);
    }

    // REP-01 y REP-02 (RN-02)
    @Test
    void existsByDniYExistsByUsuarioId() {
        repartidorRepository.save(nuevoRepartidor());

        assertThat(repartidorRepository.existsByDni("30123456")).isTrue();
        assertThat(repartidorRepository.existsByDni("99999999")).isFalse();
        assertThat(repartidorRepository.existsByUsuarioId(10L)).isTrue();
        assertThat(repartidorRepository.existsByUsuarioId(99L)).isFalse();
    }

    // REP-04: un repartidor que nunca recibio entregas (ultimaAsignacion null)
    // es el que mas tiempo lleva esperando, asi que debe salir primero
    @Test
    void findFirstDisponibleEligeAlQueNuncaRecibioEntregas() {
        LocalDateTime hoy = LocalDateTime.of(2026, 10, 29, 0, 0);
        repartidorRepository.save(repartidor(1L, "1", EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, hoy.withHour(10)));
        repartidorRepository.save(repartidor(2L, "2", EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, hoy.withHour(9)));
        repartidorRepository.save(repartidor(3L, "3", EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, null));
        // Estos tambien tienen null, pero no cumplen RN-05 / RN-06
        repartidorRepository.save(repartidor(4L, "4", EstadoRepartidor.INACTIVO, Disponibilidad.DISPONIBLE, null));
        repartidorRepository.save(repartidor(5L, "5", EstadoRepartidor.ACTIVO, Disponibilidad.NO_DISPONIBLE, null));
        repartidorRepository.save(repartidor(6L, "6", EstadoRepartidor.ACTIVO, Disponibilidad.OCUPADO, null));

        Repartidor elegido = repartidorRepository
                .findFirstByEstadoAndDisponibilidadOrderByUltimaAsignacionAsc(
                        EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE)
                .orElseThrow();

        assertThat(elegido.getUsuarioId()).isEqualTo(3L);
    }

    // REP-04 (DM-07): entre los que ya recibieron, el de asignacion mas antigua
    @Test
    void findFirstDisponibleEligeLaAsignacionMasAntigua() {
        LocalDateTime hoy = LocalDateTime.of(2026, 10, 29, 0, 0);
        repartidorRepository.save(repartidor(1L, "1", EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, hoy.withHour(10)));
        repartidorRepository.save(repartidor(2L, "2", EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, hoy.withHour(8)));
        repartidorRepository.save(repartidor(3L, "3", EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE, hoy.withHour(9)));

        Repartidor elegido = repartidorRepository
                .findFirstByEstadoAndDisponibilidadOrderByUltimaAsignacionAsc(
                        EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE)
                .orElseThrow();

        assertThat(elegido.getUsuarioId()).isEqualTo(2L);
    }

    // REP-04: sin candidatos devuelve vacio (CU-03 A4: la entrega queda pendiente)
    @Test
    void findFirstDisponibleSinCandidatosDevuelveVacio() {
        repartidorRepository.save(repartidor(1L, "1", EstadoRepartidor.ACTIVO, Disponibilidad.NO_DISPONIBLE, null));

        assertThat(repartidorRepository
                .findFirstByEstadoAndDisponibilidadOrderByUltimaAsignacionAsc(
                        EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE))
                .isEmpty();
    }

    private Repartidor repartidor(Long usuarioId, String dni, EstadoRepartidor estado,
                                  Disponibilidad disponibilidad, LocalDateTime ultimaAsignacion) {
        Repartidor repartidor = nuevoRepartidor();
        repartidor.setUsuarioId(usuarioId);
        repartidor.setDni(dni);
        repartidor.setEstado(estado);
        repartidor.setDisponibilidad(disponibilidad);
        repartidor.setUltimaAsignacion(ultimaAsignacion);
        return repartidor;
    }

    private Repartidor nuevoRepartidor() {
        Repartidor repartidor = new Repartidor();
        repartidor.setUsuarioId(10L);
        repartidor.setNombre("Juan");
        repartidor.setApellido("Perez");
        repartidor.setDni("30123456");
        repartidor.setTelefono("1155554444");
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.NO_DISPONIBLE);
        repartidor.setFechaAlta(LocalDateTime.now());
        return repartidor;
    }

    private Vehiculo nuevoVehiculo(TipoVehiculo tipo, String patente) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(tipo);
        vehiculo.setPatente(patente);
        vehiculo.setActivo(true);
        return vehiculo;
    }
}
