package ar.edu.uade.deliverygo.logistica.repository;

import ar.edu.uade.deliverygo.logistica.model.Entrega;
import ar.edu.uade.deliverygo.logistica.model.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    // REP-05: el repartidor tiene una entrega activa? (RN-07)
    boolean existsByRepartidorIdAndEstadoIn(
            Long repartidorId,
            List<EstadoEntrega> estados
    );

    // REP-06: entrega no cancelada de un pedido, para no duplicarla y
    // reutilizar la que quedo pendiente (RN-11, DM-11)
    Optional<Entrega> findByPedidoIdAndEstadoNot(
            Long pedidoId,
            EstadoEntrega estado
    );
}
