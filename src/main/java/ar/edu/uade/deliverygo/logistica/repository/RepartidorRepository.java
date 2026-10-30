package ar.edu.uade.deliverygo.logistica.repository;

import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    // REP-01: DNI unico (RN-02)
    boolean existsByDni(String dni);

    // REP-02: un usuario puede tener un solo repartidor (RN-02)
    boolean existsByUsuarioId(Long usuarioId);

    // REP-04: repartidor ACTIVO y DISPONIBLE que hace mas tiempo no recibe
    // una entrega (RN-05, RN-06, DM-07)
    Optional<Repartidor> findFirstByEstadoAndDisponibilidadOrderByUltimaAsignacionAsc(
            EstadoRepartidor estado,
            Disponibilidad disponibilidad
    );
}
