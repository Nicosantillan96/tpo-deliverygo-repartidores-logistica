package ar.edu.uade.deliverygo.logistica.config;

import ar.edu.uade.deliverygo.logistica.excepcion.CodigoError;
import ar.edu.uade.deliverygo.logistica.excepcion.ReglaNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

// Valida el JWT que emite el modulo Usuarios (HS256) y devuelve el usuarioId (claim "sub").
// Se implementa con javax.crypto para no agregar dependencias: HMAC-SHA256 sobre "header.payload"
// con los bytes UTF-8 del secreto compartido, igual que Keys.hmacShaKeyFor de jjwt en el emisor.
// No controla roles (decision del proyecto).
// Cualquier problema (formato, firma, exp vencido, sub no numerico) -> 401 UNAUTHORIZED
@Component
public class JwtValidador {

    private final byte[] secreto;
    private final ObjectMapper objectMapper;

    public JwtValidador(@Value("${logistica.jwt.secret:}") String secreto, ObjectMapper objectMapper) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    // Sin secreto configurado el Bearer se ignora y se usa X-Usuario-Id
    public boolean habilitado() {
        return secreto.length > 0;
    }

    public Long validar(String token) {
        String[] partes = token.split("\\.", -1);
        if (partes.length != 3) {
            throw noAutorizado("Token con formato inválido");
        }
        try {
            // Se verifica la firma antes de leer el contenido. isEqual compara en tiempo constante
            byte[] esperada = firmar(partes[0] + "." + partes[1]);
            byte[] recibida = Base64.getUrlDecoder().decode(partes[2]);
            if (!MessageDigest.isEqual(esperada, recibida)) {
                throw noAutorizado("Firma del token inválida");
            }
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(partes[0]));
            // Evita aceptar tokens que declaren otro algoritmo (por ejemplo "none")
            if (!"HS256".equals(header.path("alg").asText())) {
                throw noAutorizado("Algoritmo del token no soportado");
            }
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(partes[1]));
            // exp viene en segundos; es obligatorio para no aceptar tokens eternos
            if (!payload.path("exp").isNumber()
                    || payload.get("exp").asLong() <= Instant.now().getEpochSecond()) {
                throw noAutorizado("Token vencido");
            }
            return Long.valueOf(payload.path("sub").asText());
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            // Base64 mal formado, JSON invalido o sub no numerico
            throw noAutorizado("Token inválido");
        }
    }

    private byte[] firmar(String datos) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
        return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
    }

    private ReglaNegocioException noAutorizado(String mensaje) {
        return new ReglaNegocioException(CodigoError.UNAUTHORIZED, mensaje);
    }
}
