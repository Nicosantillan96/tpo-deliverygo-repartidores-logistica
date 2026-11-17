# DeliveryGo — Repartidores y Logística

Módulo **Repartidores y Logística** del sistema DeliveryGo, desarrollado como trabajo práctico de la materia Aplicaciones Interactivas (APIS) de UADE. Grupo 08.

El módulo administra a los repartidores y el ciclo de vida de cada entrega: alta del repartidor, disponibilidad, asignación de pedidos, retiro en el comercio y confirmación de la entrega con un código.

## Tecnologías

- Java 25
- Spring Boot 3.5 (Web, Data JPA, Validation)
- H2 en memoria
- springdoc-openapi (Swagger UI)
- Maven (con Maven Wrapper)
- JUnit 5, AssertJ y MockMvc para los tests

## Casos de uso implementados

| CU | Descripción | Endpoint | Respuesta |
|---|---|---|---|
| CU-01 | Registrar repartidor | `POST /api/v1/couriers` | 201 |
| CU-02 | Cambiar disponibilidad del repartidor | `PATCH /api/v1/couriers/{id}/availability` | 200 |
| CU-03 | Asignar repartidor a una entrega | `POST /api/v1/deliveries/assign` | 201 asignada / 202 pendiente |
| CU-04 | Registrar retiro del pedido | `POST /api/v1/deliveries/{id}/pickup` | 200 |
| CU-05 | Confirmar entrega del pedido | `POST /api/v1/deliveries/{id}/complete` | 200 |
| Integración | Consultar repartidor (lo usa Opiniones/Analítica) | `GET /api/v1/couriers/{id}` | 200 / 404 |
| Integración | Estado del módulo y de su base de datos | `GET /status` | 200 UP / 503 DOWN |

El `pedidoId` es un UUID, porque así identifica los pedidos el módulo Pedidos. Los demás ids (`clienteId`, `comercioId`, `usuarioId` y los propios del módulo) son números.

Las operaciones del repartidor (CU-02, CU-04 y CU-05) identifican al usuario con el header `X-Usuario-Id`, que simula el `usuarioId` del JWT que emitirá el módulo Usuarios.

## Cómo ejecutarlo

Requisitos: JDK 25. Maven no hace falta instalarlo, porque se usa el wrapper incluido.

```powershell
# Windows (PowerShell), si JAVA_HOME no está definido
$env:JAVA_HOME = "C:\ruta\al\jdk-25"

# Correr los tests
.\mvnw test

# Levantar la aplicación (base vacía)
.\mvnw spring-boot:run

# Levantar la aplicación con datos de prueba
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=demo"
```

En Linux o macOS se usa `./mvnw` en lugar de `.\mvnw`.

**Puerto: 8088** (Grupo 8). Está fijo en `application.properties` para no chocar con los otros módulos en la máquina de integración: Usuarios y Pedidos usan el 8080, Catálogo el 8082 y Notificaciones el 8089. Se puede cambiar con la variable de entorno `SERVER_PORT`.

Con la aplicación levantada:

- Estado: http://localhost:8088/status → `{ "status": "UP", "modulo": "deliverygo-logistica", "baseDeDatos": "UP", ... }`
- Swagger UI: http://localhost:8088/swagger-ui.html
- Contrato OpenAPI: http://localhost:8088/v3/api-docs.yaml
- Consola de H2: http://localhost:8088/h2-console (JDBC URL `jdbc:h2:mem:logistica`, usuario `sa`, sin contraseña)

La base es en memoria, así que se borra cada vez que se detiene la aplicación.

## Datos de prueba (perfil `demo`)

Con el perfil `demo`, `DemoDataLoader` carga estos datos a través de los services, aplicando las mismas reglas de negocio que la API:

| Repartidor | Vehículo | Estado que deja |
|---|---|---|
| Usuario 1 | MOTO | Entrega del pedido 100 ASIGNADA (se puede registrar el retiro) |
| Usuario 2 | BICICLETA | Entrega del pedido 101 RETIRADA (se puede confirmar) |
| Usuario 3 | AUTO | DISPONIBLE (recibe la próxima asignación) |

Al arrancar, la consola muestra los ids y los códigos de confirmación de las dos entregas. La API nunca devuelve ese código; en el sistema real lo recibe el cliente por Notificaciones.

Ejemplo de recorrido con los datos de demo:

```bash
# CU-04: el repartidor del usuario 1 retira el pedido de la entrega 1
curl -X POST http://localhost:8088/api/v1/deliveries/1/pickup \
  -H "X-Usuario-Id: 1"

# CU-05: el repartidor del usuario 2 confirma la entrega 2 con el código de la consola
curl -X POST http://localhost:8088/api/v1/deliveries/2/complete \
  -H "X-Usuario-Id: 2" -H "Content-Type: application/json" \
  -d '{ "codigoConfirmacion": "1234" }'

# CU-03: se asigna el pedido 102; lo toma el repartidor disponible
curl -X POST http://localhost:8088/api/v1/deliveries/assign \
  -H "Content-Type: application/json" \
  -d '{ "pedidoId": "00000000-0000-0000-0000-000000000102", "clienteId": 202, "comercioId": 300,
        "direccionRetiro": "Av. Corrientes 1234, CABA",
        "direccionEntrega": "Lima 757, CABA" }'
```

## Integraciones con otros módulos

Los demás módulos de DeliveryGo todavía no están disponibles, así que cada integración es una interfaz (`client`) con una implementación simulada (mock). Cuando exista el módulo real, se agrega una implementación HTTP sin modificar los services.

| Módulo | Mock | Comportamiento |
|---|---|---|
| Usuarios | `UsuariosClientMock` | Usuarios 1 a 5 elegibles; 6 inactivo; 7 con rol CLIENTE; el resto no existe |
| Pedidos | `PedidosClientMock` | Pedidos 100 a 105 CONFIRMADOS; 106 PENDIENTE; el resto no existe. Son UUID con el número al final: el 100 es `00000000-0000-0000-0000-000000000100` |
| Notificaciones | `NotificacionesClientMock` | Solo escribe el aviso en el log |
| Analítica | `AnaliticaClientMock` | Solo escribe el evento en el log |

Si un aviso a otro módulo falla, la operación no se revierte; el error queda en el log.

### Perfil `integracion` (módulos reales)

Con el perfil `integracion`, Logística consulta al módulo **Usuarios real** en lugar del mock (`UsuariosClientHttp`): pide su token de módulo (`POST /api/v1/auth/module-token`) y valida cada usuario con `GET /api/v1/users/{id}/access`. Si Usuarios no responde, CU-01 devuelve 503 `USERS_SERVICE_UNAVAILABLE`.

También usa **Notificaciones real** (`NotificacionesClientHttp`): `POST /api/v1/notifications` con el `usuarioId` del destinatario. Al asignar, el cliente recibe el código de confirmación; al retirar y al entregar, un aviso. Notificaciones solo acepta el JWT de una persona, así que Logística reenvía el `Authorization` del pedido en curso. Las plantillas las crea un ADMIN en Notificaciones y sus ids se pasan por variables; si una falta, ese aviso se omite. Pedidos y Analítica siguen con sus mocks.

```bash
USUARIOS_BASE_URL=http://localhost:8081 MODULE_KEY_LOGISTICA=<clave del Grupo 2> JWT_SECRET=<secreto JWT de Usuarios> \
NOTIFICACIONES_BASE_URL=http://localhost:8089 \
NOTIF_PLANTILLA_ASIGNACION_CLIENTE=<id> NOTIF_PLANTILLA_ASIGNACION_REPARTIDOR=<id> \
NOTIF_PLANTILLA_RETIRO=<id> NOTIF_PLANTILLA_ENTREGA=<id> \
  ./mvnw spring-boot:run "-Dspring-boot.run.profiles=integracion"
```

Variables de las plantillas: asignación al cliente `{entregaId}` y `{codigoConfirmacion}`; las demás, `{entregaId}`.

En la integración no conviene usar el perfil `demo`: registra repartidores para los usuarios 1, 2 y 3, que en Usuarios real son otras personas.

### Identidad del repartidor (JWT)

CU-02, CU-04 y CU-05 toman el usuario del JWT de Usuarios (`Authorization: Bearer <token>`, claim `sub`), validado con el secreto compartido `JWT_SECRET` (HS256). Un token inválido o vencido responde 401 `UNAUTHORIZED`. Sin token se acepta el header `X-Usuario-Id` (pruebas y demo).

### Trazabilidad

Cada pedido lleva el header `X-Correlation-Id`: si viene se usa ese valor y si no se genera uno. Se devuelve en la respuesta, aparece en cada línea del log y en el `correlationId` de los errores, y se reenvía a Usuarios.

El estado del módulo también responde en `GET /api/v1/status`, la misma ruta que usan Usuarios y Analítica.

CORS está habilitado para `/api/**` (`CorsConfig`), así el Frontend puede llamar a la API desde el navegador. Los orígenes se configuran con `logistica.cors.allowed-origins` (por defecto, cualquiera).

## Arquitectura

Paquete base `ar.edu.uade.deliverygo.logistica`:

| Paquete | Responsabilidad |
|---|---|
| `controller` | Recibe las solicitudes HTTP, valida los requests con `@Valid` y delega en el service |
| `service` | Casos de uso, reglas de negocio que requieren consultas y transacciones |
| `model` | Entidades JPA, que protegen sus propias reglas (máquina de estados, disponibilidad) |
| `repository` | Acceso a la base con Spring Data JPA |
| `dto` | Contratos de entrada (`...Request`) y de salida (`...Response`) |
| `mapper` | Conversión de entidad a Response |
| `client` | Integraciones con otros módulos |
| `excepcion` | `ReglaNegocioException`, `CodigoError` y `GlobalExceptionHandler` |
| `config` | Swagger (`OpenApiConfig`) y datos de demo (`DemoDataLoader`) |

Decisiones principales:

- Las entidades nunca se exponen en la API: entra un `Request` validado y sale un `Response`. `EntregaResponse` no incluye el código de confirmación.
- Las entregas siguen una máquina de estados: `PENDIENTE_ASIGNACION → ASIGNADA → RETIRADA → COMPLETADA`. Cada cambio queda registrado en el historial con el estado anterior, el nuevo, la fecha y el actor.
- Un repartidor solo puede tener una entrega activa. Mientras la tiene queda `OCUPADO` y al completarla vuelve a `DISPONIBLE`.
- La concurrencia en la asignación se controla con `@Version` (bloqueo optimista): si dos asignaciones toman al mismo repartidor, la segunda recibe 409.

## Errores

Todos los errores de negocio y de validación responden con el mismo formato:

```json
{
  "timestamp": "2026-11-17T10:15:30",
  "status": 409,
  "code": "COURIER_DNI_DUPLICATED",
  "message": "Ya existe un repartidor con el DNI 30123456",
  "path": "/api/v1/couriers",
  "correlationId": "5b0c1f3e-8a7d-4c2e-9f1a-2d3b4c5e6f70"
}
```

Cada código de error (`CodigoError`) tiene asociado su estado HTTP: 400 datos inválidos, 403 usuario no autorizado, 404 recurso inexistente, 409 conflicto con el estado actual y 422 regla de negocio no cumplida. El listado completo está en Swagger UI.

## Tests

```powershell
.\mvnw test
```

Incluyen tests de entidades, repositorios, services (con la base H2 real), validación de requests, contratos JSON, manejo de errores, controllers de punta a punta con MockMvc, la configuración de OpenAPI y la carga de datos de demo.

## Documentación

- `doc/Grupo08_Primera_Entrega_Repartidores_y_Logistica.docx`: especificación funcional y técnica del módulo (primera entrega).
- `doc/openapi.yaml`: contrato OpenAPI exportado de la API.

## Pendiente

- Casos de uso de evolución (CU-06 a CU-10): aceptar o rechazar la asignación, informar pedido en camino, seguimiento, administración de vehículos y de zonas.
- Reemplazar los mocks de Pedidos y Analítica por clientes HTTP (Usuarios y Notificaciones ya tienen el suyo en el perfil `integracion`). Para Pedidos falta acordar cómo consultar un pedido sin `X-Cliente-Id`.
