package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.dto.RepartidorDTO;
import ar.edu.uade.deliverygo.logistica.model.Disponibilidad;
import ar.edu.uade.deliverygo.logistica.model.EstadoRepartidor;
import ar.edu.uade.deliverygo.logistica.model.Repartidor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.edu.uade.deliverygo.logistica.repository.RepartidorRepository;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class RepartidorService {

    private final RepartidorRepository repartidorRepository;

    public RepartidorService(RepartidorRepository repartidorRepository) {
        this.repartidorRepository = repartidorRepository;
    }

    @Transactional
    public RepartidorDTO registrarRepartidor(RepartidorDTO repartidorDTO) {

        // Validar que el DTO no sea nulo
        if (Objects.isNull(repartidorDTO)) {
            throw new IllegalArgumentException(
                    "Los datos del repartidor son obligatorios"
            );
        }

        // Obtener y validar el DNI
        String dni = repartidorDTO.getDni();

        if (Objects.isNull(dni) || dni.isBlank()) {
            throw new IllegalArgumentException(
                    "El DNI del repartidor es obligatorio"
            );
        }

        // Comprobar si el DNI ya existe
        if (repartidorRepository.existsByDni(dni)) {
            throw new IllegalArgumentException(
                    "Ya existe un repartidor con ese DNI"
            );
        }

        // Validar los datos obligatorios
        String nombre = repartidorDTO.getNombre();
        String apellido = repartidorDTO.getApellido();
        String telefono = repartidorDTO.getTelefono();

        if (Objects.isNull(nombre) || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del repartidor es obligatorio"
            );
        }

        if (Objects.isNull(apellido) || apellido.isBlank()) {
            throw new IllegalArgumentException(
                    "El apellido del repartidor es obligatorio"
            );
        }

        if (Objects.isNull(telefono) || telefono.isBlank()) {
            throw new IllegalArgumentException(
                    "El teléfono del repartidor es obligatorio"
            );
        }

        // Crear la entidad
        Repartidor repartidor = new Repartidor();

        repartidor.setNombre(nombre);
        repartidor.setApellido(apellido);
        repartidor.setDni(dni);
        repartidor.setTelefono(telefono);

        // Establecer los valores iniciales
        repartidor.setEstado(EstadoRepartidor.ACTIVO);
        repartidor.setDisponibilidad(Disponibilidad.NO_DISPONIBLE);
        repartidor.setFechaAlta(LocalDateTime.now());

        repartidorRepository.save(repartidor);
        return repartidorDTO;
    }
}

