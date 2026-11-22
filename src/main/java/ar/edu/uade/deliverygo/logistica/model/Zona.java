package ar.edu.uade.deliverygo.logistica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// CU-10: zona de operacion. Cada repartidor trabaja en una zona (opcional) y la
// asignacion (CU-03) filtra por la zona de la entrega. Se agrega recien ahora (DM-06)
@Entity
@Table(name = "zonas")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El nombre identifica a la zona: no puede repetirse
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    public Zona() {
    }

    public Zona(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
