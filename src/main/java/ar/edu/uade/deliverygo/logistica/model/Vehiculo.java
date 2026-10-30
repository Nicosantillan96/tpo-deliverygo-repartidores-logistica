package ar.edu.uade.deliverygo.logistica.model;

import jakarta.persistence.*;

@Entity
@Table (name = "vehiculos")

public class Vehiculo {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated (EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipo;
    //obligatoria solo para MOTO y AUTO (RN-04), se valida en el service
    @Column(unique = true)
    private String patente;
    
    private String descripcion;
    @Column(nullable = false)
    private boolean activo;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "repartidor_id",nullable = false)
    private Repartidor repartidor;

    public Vehiculo(){
    }
// Getters y setters

    public Long getId() {
        return id;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) {
        this.patente = patente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Repartidor getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(Repartidor repartidor) {
        this.repartidor = repartidor;
    }
}
