package ar.edu.uade.deliverygo.logistica.model;

import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepartidorTest {

    // CU-02

    @Test
    void cambiarDisponibilidadADisponible() {
        Repartidor repartidor = repartidor(EstadoRepartidor.ACTIVO, Disponibilidad.NO_DISPONIBLE);

        repartidor.cambiarDisponibilidad(Disponibilidad.DISPONIBLE);

        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    @Test
    void noSePuedePedirOcupadoManualmente() {
        Repartidor repartidor = repartidor(EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE);

        assertThatThrownBy(() -> repartidor.cambiarDisponibilidad(Disponibilidad.OCUPADO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.VALIDATION_ERROR);
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    @Test
    void unRepartidorInactivoNoPuedeCambiarDisponibilidad() {
        Repartidor repartidor = repartidor(EstadoRepartidor.INACTIVO, Disponibilidad.NO_DISPONIBLE);

        assertThatThrownBy(() -> repartidor.cambiarDisponibilidad(Disponibilidad.DISPONIBLE))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.COURIER_INACTIVE);
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.NO_DISPONIBLE);
    }

    @Test
    void unRepartidorOcupadoNoPuedeCambiarDisponibilidad() {
        Repartidor repartidor = repartidor(EstadoRepartidor.ACTIVO, Disponibilidad.OCUPADO);

        assertThatThrownBy(() -> repartidor.cambiarDisponibilidad(Disponibilidad.NO_DISPONIBLE))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.COURIER_HAS_ACTIVE_DELIVERY);
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.OCUPADO);
    }

    // CU-03 y CU-05

    @Test
    void ocuparYLiberar() {
        Repartidor repartidor = repartidor(EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE);

        repartidor.ocupar();

        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.OCUPADO);
        assertThat(repartidor.getUltimaAsignacion()).isNotNull();

        repartidor.liberar();

        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    @Test
    void noSePuedeOcuparUnRepartidorNoDisponible() {
        Repartidor repartidor = repartidor(EstadoRepartidor.ACTIVO, Disponibilidad.NO_DISPONIBLE);

        assertThatThrownBy(repartidor::ocupar)
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.COURIER_NOT_AVAILABLE);
        assertThat(repartidor.getUltimaAsignacion()).isNull();
    }

    @Test
    void noSePuedeOcuparUnRepartidorInactivo() {
        Repartidor repartidor = repartidor(EstadoRepartidor.INACTIVO, Disponibilidad.DISPONIBLE);

        assertThatThrownBy(repartidor::ocupar)
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.COURIER_NOT_AVAILABLE);
    }

    @Test
    void vehiculoActivoDevuelveElMarcadoComoActivo() {
        Repartidor repartidor = repartidor(EstadoRepartidor.ACTIVO, Disponibilidad.DISPONIBLE);
        Vehiculo viejo = vehiculo(TipoVehiculo.BICICLETA, false);
        Vehiculo actual = vehiculo(TipoVehiculo.MOTO, true);
        repartidor.agregarVehiculo(viejo);
        repartidor.agregarVehiculo(actual);

        assertThat(repartidor.vehiculoActivo()).isSameAs(actual);
    }

    private Repartidor repartidor(EstadoRepartidor estado, Disponibilidad disponibilidad) {
        Repartidor repartidor = new Repartidor();
        repartidor.setEstado(estado);
        repartidor.setDisponibilidad(disponibilidad);
        return repartidor;
    }

    private Vehiculo vehiculo(TipoVehiculo tipo, boolean activo) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(tipo);
        vehiculo.setActivo(activo);
        return vehiculo;
    }
}
