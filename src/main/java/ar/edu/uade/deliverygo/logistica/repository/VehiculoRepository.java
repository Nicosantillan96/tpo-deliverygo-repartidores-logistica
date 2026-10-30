package ar.edu.uade.deliverygo.logistica.repository;

import ar.edu.uade.deliverygo.logistica.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
}
