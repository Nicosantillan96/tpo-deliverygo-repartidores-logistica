package ar.edu.uade.deliverygo.logistica.mapper;

import ar.edu.uade.deliverygo.logistica.dto.EntregaResponse;
import ar.edu.uade.deliverygo.logistica.dto.RepartidorResponse;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.Entrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import ar.edu.uade.deliverygo.logistica.model.TipoVehiculo;
import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Conversion entidad -> Response, sin Spring ni base de datos
class MappersTest {

    private final RepartidorMapper repartidorMapper = new RepartidorMapper();
    private final EntregaMapper entregaMapper = new EntregaMapper();

    // DTO-02: copia los datos e incluye solo el vehiculo activo (REL-01)
    @Test
    void elRepartidorSeConvierteConSuVehiculoActivo() {
        Repartidor repartidor = new Repartidor();
        repartidor.setUsuarioId(1L);
        repartidor.setNombre("Juan");
        repartidor.setDni("30123456");
        repartidor.setTelefono("1155554444");
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.NO_DISPONIBLE);
        repartidor.agregarVehiculo(vehiculo(TipoVehiculo.BICICLETA, null, false));
        repartidor.agregarVehiculo(vehiculo(TipoVehiculo.MOTO, "AB123CD", true));

        RepartidorResponse response = repartidorMapper.toResponse(repartidor);

        assertThat(response.getUsuarioId()).isEqualTo(1L);
        assertThat(response.getDni()).isEqualTo("30123456");
        assertThat(response.getEstado()).isEqualTo(EstadoRepartidor.ACTIVO);
        assertThat(response.getVehiculo().getTipo()).isEqualTo(TipoVehiculo.MOTO);
        assertThat(response.getVehiculo().getPatente()).isEqualTo("AB123CD");
    }

    @Test
    void unRepartidorSinVehiculoActivoDevuelveVehiculoNull() {
        Repartidor repartidor = new Repartidor();

        assertThat(repartidorMapper.toResponse(repartidor).getVehiculo()).isNull();
    }

    // DTO-05: una entrega PENDIENTE todavia no tiene repartidor
    @Test
    void unaEntregaSinRepartidorDevuelveRepartidorIdNull() {
        Entrega entrega = new Entrega();
        entrega.setPedidoId(100L);
        entrega.setEstado(EstadoEntrega.PENDIENTE_ASIGNACION);
        entrega.setFechaCreacion(LocalDateTime.of(2026, 11, 3, 10, 0));

        EntregaResponse response = entregaMapper.toResponse(entrega);

        assertThat(response.getPedidoId()).isEqualTo(100L);
        assertThat(response.getEstado()).isEqualTo(EstadoEntrega.PENDIENTE_ASIGNACION);
        assertThat(response.getRepartidorId()).isNull();
    }

    private Vehiculo vehiculo(TipoVehiculo tipo, String patente, boolean activo) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(tipo);
        vehiculo.setPatente(patente);
        vehiculo.setActivo(activo);
        return vehiculo;
    }
}
