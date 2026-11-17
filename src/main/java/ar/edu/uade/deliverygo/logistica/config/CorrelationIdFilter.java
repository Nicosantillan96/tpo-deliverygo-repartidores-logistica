package ar.edu.uade.deliverygo.logistica.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

// Trazabilidad de punta a punta (paso 8 de la integracion): cada operacion lleva un
// identificador en el header X-Correlation-Id, el mismo en todos los modulos.
// - Si el pedido trae el header (lo manda otro modulo o el Frontend), se usa ese valor.
// - Si no lo trae, Logistica genera uno nuevo.
// El valor se guarda en el MDC del log (aparece en cada linea), se devuelve en la respuesta,
// lo usa el ErrorResponse y lo reenvian los clientes HTTP al llamar a otros modulos.
// Corre antes que todo lo demas (HIGHEST_PRECEDENCE) para que cubra el request completo
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    // Un valor recibido mas largo que esto se descarta, para no llenar el log con basura
    private static final int LARGO_MAXIMO = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (Objects.isNull(correlationId) || correlationId.isBlank()
                || correlationId.length() > LARGO_MAXIMO) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            // El hilo se reutiliza para otros pedidos: hay que limpiar el MDC
            MDC.remove(MDC_KEY);
        }
    }

    // Correlation id del pedido en curso. Fuera de un pedido HTTP (tests, tareas al arrancar)
    // no hay uno, y se genera uno nuevo
    public static String actual() {
        String correlationId = MDC.get(MDC_KEY);
        return Objects.isNull(correlationId) ? UUID.randomUUID().toString() : correlationId;
    }
}
