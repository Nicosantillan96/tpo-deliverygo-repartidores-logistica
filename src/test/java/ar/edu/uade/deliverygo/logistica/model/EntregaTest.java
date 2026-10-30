package ar.edu.uade.deliverygo.logistica.model;

import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntregaTest {

    private static final String CODIGO = "4821";

    @Test
    void flujoCompletoAsignarRetirarYCompletar() {
        Repartidor repartidor = repartidorDisponible();
        Entrega entrega = entregaPendiente();

        // CU-03
        entrega.asignar(repartidor, CODIGO);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entrega.getRepartidor()).isSameAs(repartidor);
        assertThat(entrega.getCodigoConfirmacion()).isEqualTo(CODIGO);
        assertThat(entrega.getFechaAsignacion()).isNotNull();
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.OCUPADO);

        // CU-04
        entrega.registrarRetiro();
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.RETIRADA);
        assertThat(entrega.getFechaRetiro()).isNotNull();

        // CU-05
        entrega.completar(CODIGO);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.COMPLETADA);
        assertThat(entrega.getFechaEntrega()).isNotNull();
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    // CU-03

    @Test
    void noSePuedeAsignarUnaEntregaYaAsignada() {
        Entrega entrega = entregaPendiente();
        entrega.asignar(repartidorDisponible(), CODIGO);
        Repartidor otro = repartidorDisponible();

        assertThatThrownBy(() -> entrega.asignar(otro, "1111"))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.DELIVERY_INVALID_STATE);
        // El segundo repartidor no queda ocupado
        assertThat(otro.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    @Test
    void siElRepartidorNoEstaDisponibleLaEntregaQuedaPendiente() {
        Repartidor repartidor = repartidorDisponible();
        repartidor.setDisponibilidad(Disponibilidad.NO_DISPONIBLE);
        Entrega entrega = entregaPendiente();

        assertThatThrownBy(() -> entrega.asignar(repartidor, CODIGO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.COURIER_NOT_AVAILABLE);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(entrega.getRepartidor()).isNull();
        assertThat(entrega.getCodigoConfirmacion()).isNull();
    }

    @Test
    void asignarSinCodigoValidoEsUnErrorDeProgramacion() {
        Repartidor repartidor = repartidorDisponible();
        Entrega entrega = entregaPendiente();

        assertThatThrownBy(() -> entrega.asignar(repartidor, "12"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.DISPONIBLE);
    }

    // CU-04

    @Test
    void noSePuedeRetirarUnaEntregaSinAsignar() {
        Entrega entrega = entregaPendiente();

        assertThatThrownBy(entrega::registrarRetiro)
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.DELIVERY_INVALID_STATE);
        assertThat(entrega.getFechaRetiro()).isNull();
    }

    @Test
    void noSePuedeRetirarDosVeces() {
        Entrega entrega = entregaPendiente();
        entrega.asignar(repartidorDisponible(), CODIGO);
        entrega.registrarRetiro();

        assertThatThrownBy(entrega::registrarRetiro)
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.DELIVERY_INVALID_STATE);
    }

    // CU-05

    @Test
    void noSePuedeCompletarUnaEntregaNoRetirada() {
        Repartidor repartidor = repartidorDisponible();
        Entrega entrega = entregaPendiente();
        entrega.asignar(repartidor, CODIGO);

        assertThatThrownBy(() -> entrega.completar(CODIGO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.DELIVERY_NOT_PICKED_UP);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.OCUPADO);
    }

    @Test
    void conCodigoIncorrectoLaEntregaNoCambiaDeEstado() {
        Repartidor repartidor = repartidorDisponible();
        Entrega entrega = entregaPendiente();
        entrega.asignar(repartidor, CODIGO);
        entrega.registrarRetiro();

        assertThatThrownBy(() -> entrega.completar("0000"))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.INVALID_CONFIRMATION_CODE);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.RETIRADA);
        assertThat(entrega.getFechaEntrega()).isNull();
        assertThat(repartidor.getDisponibilidad()).isEqualTo(Disponibilidad.OCUPADO);
    }

    @Test
    void noSePuedeCompletarDosVeces() {
        Entrega entrega = entregaPendiente();
        entrega.asignar(repartidorDisponible(), CODIGO);
        entrega.registrarRetiro();
        entrega.completar(CODIGO);

        assertThatThrownBy(() -> entrega.completar(CODIGO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.DELIVERY_INVALID_STATE);
    }

    @Test
    void noSePuedeCompletarUnaEntregaPendiente() {
        Entrega entrega = entregaPendiente();

        assertThatThrownBy(() -> entrega.completar(CODIGO))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.DELIVERY_INVALID_STATE);
    }

    private Entrega entregaPendiente() {
        Entrega entrega = new Entrega();
        entrega.setEstado(EstadoEntrega.PENDIENTE_ASIGNACION);
        return entrega;
    }

    private Repartidor repartidorDisponible() {
        Repartidor repartidor = new Repartidor();
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.DISPONIBLE);
        return repartidor;
    }
}
