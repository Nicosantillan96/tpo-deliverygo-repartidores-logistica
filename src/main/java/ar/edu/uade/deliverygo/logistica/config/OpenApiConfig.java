package ar.edu.uade.deliverygo.logistica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Datos generales que muestra Swagger UI (/swagger-ui.html).
// Los endpoints, los DTO y el header X-Usuario-Id los documenta springdoc solo,
// leyendo los controllers y las anotaciones de Bean Validation
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logisticaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("DeliveryGo - Repartidores y Logística")
                .version("1.0")
                .description("Módulo de repartidores y ciclo de vida de las entregas (CU-01 a CU-05). "
                        + "Las operaciones del repartidor (disponibilidad, retiro y confirmación) "
                        + "requieren el header X-Usuario-Id, que simula el usuario del JWT (RN-10). "
                        + "Usuarios de prueba del mock: 1 a 5. Pedidos confirmados del mock: 100 a 105."));
    }
}
