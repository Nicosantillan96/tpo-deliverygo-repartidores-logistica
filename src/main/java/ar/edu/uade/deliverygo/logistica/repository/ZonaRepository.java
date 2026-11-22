package ar.edu.uade.deliverygo.logistica.repository;

import ar.edu.uade.deliverygo.logistica.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// CU-10: zonas de operacion
public interface ZonaRepository extends JpaRepository<Zona, Long> {

    // REP-12: el nombre de la zona no se repite (sin distinguir mayusculas)
    boolean existsByNombreIgnoreCase(String nombre);

    // REP-13: listado ordenado por nombre
    List<Zona> findAllByOrderByNombreAsc();
}
