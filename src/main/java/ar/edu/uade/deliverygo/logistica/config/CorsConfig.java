package ar.edu.uade.deliverygo.logistica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS: permite que el Frontend (React, Grupo 6) llame a la API desde el navegador.
// Sin esto, el navegador bloquea las llamadas que vienen de otro origen (otra IP o puerto).
// Las llamadas entre modulos (servidor a servidor) no pasan por CORS.
// Los origenes permitidos se configuran en application.properties (logistica.cors.allowed-origins);
// por defecto "*", porque en la integracion por VPN no sabemos de antemano la IP del Frontend
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public CorsConfig(@Value("${logistica.cors.allowed-origins:*}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
