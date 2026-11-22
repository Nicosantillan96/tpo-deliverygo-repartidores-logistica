package ar.edu.uade.deliverygo.logistica.service;

import ar.edu.uade.deliverygo.logistica.dto.CrearZonaRequest;
import ar.edu.uade.deliverygo.logistica.dto.ZonaResponse;
import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import ar.edu.uade.deliverygo.logistica.mapper.ZonaMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CU-10: alta y listado de zonas, con la base H2 real. Cada test se revierte al terminar
@DataJpaTest
@Import({ZonaService.class, ZonaMapper.class})
class ZonaServiceTest {

    @Autowired
    private ZonaService zonaService;

    @Test
    void creaUnaZona() {
        ZonaResponse zona = zonaService.crearZona(new CrearZonaRequest("  Palermo "));

        assertThat(zona.getId()).isNotNull();
        assertThat(zona.getNombre()).isEqualTo("Palermo");
    }

    // El nombre no se repite, sin distinguir mayusculas
    @Test
    void noSePuedeRepetirElNombre() {
        zonaService.crearZona(new CrearZonaRequest("Palermo"));

        assertThatThrownBy(() -> zonaService.crearZona(new CrearZonaRequest("PALERMO")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("codigo").isEqualTo(CodigoError.ZONE_NAME_DUPLICATED);
    }

    @Test
    void listaLasZonasPorNombre() {
        zonaService.crearZona(new CrearZonaRequest("Palermo"));
        zonaService.crearZona(new CrearZonaRequest("Belgrano"));

        assertThat(zonaService.listarZonas())
                .extracting(ZonaResponse::getNombre)
                .containsExactly("Belgrano", "Palermo");
    }
}
