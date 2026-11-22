package ar.edu.uade.deliverygo.logistica.dto;

// DTO-17: salida de CU-10 (zonas). Solo lectura: no tiene setters
public class ZonaResponse {

    private final Long id;
    private final String nombre;

    public ZonaResponse(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    //GETTERS
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
