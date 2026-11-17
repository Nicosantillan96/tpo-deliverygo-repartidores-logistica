package ar.edu.uade.deliverygo.logistica.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

// Registra el resolver de @UsuarioAutenticado (RN-10)
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UsuarioAutenticadoResolver usuarioResolver;

    public WebMvcConfig(UsuarioAutenticadoResolver usuarioResolver) {
        this.usuarioResolver = usuarioResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(usuarioResolver);
    }
}
