# DeliveryGo — Repartidores y Logística

Módulo **Repartidores y Logística** del sistema DeliveryGo, desarrollado como trabajo práctico de la materia Aplicaciones Interactivas (APIS) de UADE. Grupo 08.

El módulo administra a los repartidores y el ciclo de vida de cada entrega: alta del repartidor, disponibilidad, asignación de pedidos (con aceptación o rechazo), retiro en el comercio, viaje hacia el cliente y confirmación de la entrega con un código. También permite seguir cada entrega con su historial y administrar los vehículos y las zonas de operación de los repartidores.

> **Ramas:** `examen` es la versión completa que se presenta (CU-01 a CU-10, con mocks de los demás módulos y datos de demo). `main` muestra la integración real con los otros módulos de DeliveryGo.

## Tecnologías

- Java 25
- Spring Boot 3.5 (Web, Data JPA, Validation)
- H2 en memoria
- springdoc-openapi (Swagger UI)
- Maven (con Maven Wrapper)
- JUnit 5, AssertJ y MockMvc para los tests

## Casos de uso implementados

**CORE**

| CU | Descripción | Endpoint | Respuesta |
|---|---|---|---|
| CU-01 | Registrar repartidor | `POST /api/v1/couriers` | 201 |
| CU-02 | Cambiar disponibilidad del repartidor | `PATCH /api/v1/couriers/{id}/availability` | 200 |
| CU-03 | Asignar repartidor a una entrega | `POST /api/v1/deliveries/assign` | 201 asignada / 202 pendiente |
| CU-04 | Registrar retiro del pedido | `POST /api/v1/deliveries/{id}/pickup` | 200 |
| CU-05 | Confirmar entrega del pedido | `POST /api/v1/deliveries/{id}/complete` | 200 |

**Evolución**

| CU | Descripción | Endpoint | Respuesta |
|---|---|---|---|
| CU-06 | Aceptar la asignación | `POST /api/v1/deliveries/{id}/accept` | 200 |
| CU-06 | Rechazar la asignación (se reasigna a otro repartidor) | `POST /api/v1/deliveries/{id}/reject` | 200 |
| CU-07 | Informar pedido en camino | `POST /api/v1/deliveries/{id}/on-the-way` | 200 |
| CU-08 | Consultar seguimiento de la entrega (con historial) | `GET /api/v1/deliveries/{id}` | 200 |
| CU-08 | Consultar repartidor | `GET /api/v1/couriers/{id}` | 200 |
| CU-09 | Listar vehículos del repartidor | `GET /api/v1/couriers/{id}/vehicles` | 200 |
| CU-09 | Agregar vehículo | `POST /api/v1/couriers/{id}/vehicles` | 201 |
| CU-09 | Activar vehículo | `PATCH /api/v1/couriers/{id}/vehicles/{vehiculoId}/activate` | 200 |
| CU-09 | Quitar vehículo | `DELETE /api/v1/couriers/{id}/vehicles/{vehiculoId}` | 204 |
| CU-10 | Crear zona | `POST /api/v1/zones` | 201 |
| CU-10 | Listar zonas | `GET /api/v1/zones` | 200 |
| CU-10 | Asignar zona al repartidor | `PUT /api/v1/couriers/{id}/zone` | 200 |

Las operaciones del repartidor (CU-02, CU-04 a CU-07, CU-09 y CU-10) identifican al usuario con el header `X-Usuario-Id`, que simula el `usuarioId` del JWT que emitirá el módulo Usuarios. Solo el titular del repartidor, o el repartidor asignado a la entrega, puede hacerlas.

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

Con la aplicación levantada:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Contrato OpenAPI: http://localhost:8080/v3/api-docs.yaml
- Consola de H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:logistica`, usuario `sa`, sin contraseña)

La base es en memoria, así que se borra cada vez que se detiene la aplicación.

## Datos de prueba (perfil `demo`)

Con el perfil `demo`, `DemoDataLoader` carga estos datos a través de los services, aplicando las mismas reglas de negocio que la API:

| Repartidor | Vehículo | Estado que deja | Para probar |
|---|---|---|---|
| Usuario 1 | MOTO | Entrega del pedido 100 ASIGNADA | CU-06 aceptar o rechazar, CU-04 retiro |
| Usuario 2 | BICICLETA | Entrega del pedido 101 RETIRADA | CU-07 en camino, CU-05 confirmación |
| Usuario 3 | AUTO (y una bicicleta inactiva) | DISPONIBLE, en la zona Centro | Próxima asignación, CU-09 vehículos, CU-10 zona |

También crea las zonas **Centro** y **Palermo** (en Palermo no hay repartidores, así que una entrega de esa zona queda pendiente). Cualquier entrega sirve para el seguimiento (CU-08). Los usuarios 4 y 5 quedan libres para registrar repartidores nuevos.

Al arrancar, la consola muestra los ids, los códigos de confirmación de las dos entregas y qué probar con cada una. La API nunca devuelve ese código; en el sistema real lo recibe el cliente por Notificaciones.

Ejemplo de recorrido con los datos de demo:

```bash
# CU-06: el repartidor del usuario 1 acepta la entrega 1
curl -X POST http://localhost:8080/api/v1/deliveries/1/accept \
  -H "X-Usuario-Id: 1"

# CU-04: y retira el pedido
curl -X POST http://localhost:8080/api/v1/deliveries/1/pickup \
  -H "X-Usuario-Id: 1"

# CU-07: el repartidor del usuario 2 informa que va en camino con la entrega 2
curl -X POST http://localhost:8080/api/v1/deliveries/2/on-the-way \
  -H "X-Usuario-Id: 2"

# CU-05: y la confirma con el código de la consola
curl -X POST http://localhost:8080/api/v1/deliveries/2/complete \
  -H "X-Usuario-Id: 2" -H "Content-Type: application/json" \
  -d '{ "codigoConfirmacion": "1234" }'

# CU-03: se asigna el pedido 102; lo toma el repartidor disponible
curl -X POST http://localhost:8080/api/v1/deliveries/assign \
  -H "Content-Type: application/json" \
  -d '{ "pedidoId": 102, "clienteId": 202, "comercioId": 300,
        "direccionRetiro": "Av. Corrientes 1234, CABA",
        "direccionEntrega": "Lima 757, CABA" }'

# CU-08: seguimiento de la entrega 2 con su historial
curl http://localhost:8080/api/v1/deliveries/2
```

## Integraciones con otros módulos

Los demás módulos de DeliveryGo todavía no están disponibles, así que cada integración es una interfaz (`client`) con una implementación simulada (mock). Cuando exista el módulo real, se agrega una implementación HTTP sin modificar los services.

| Módulo | Mock | Comportamiento |
|---|---|---|
| Usuarios | `UsuariosClientMock` | Usuarios 1 a 5 elegibles; 6 inactivo; 7 con rol CLIENTE; el resto no existe |
| Pedidos | `PedidosClientMock` | Pedidos 100 a 105 CONFIRMADOS; 106 PENDIENTE; el resto no existe |
| Notificaciones | `NotificacionesClientMock` | Solo escribe el aviso en el log (asignación, retiro, en camino y entrega) |
| Analítica | `AnaliticaClientMock` | Solo escribe el evento en el log |

Si un aviso a otro módulo falla, la operación no se revierte; el error queda en el log.

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
- Las entregas siguen una máquina de estados: `PENDIENTE_ASIGNACION → ASIGNADA → RETIRADA → EN_CAMINO → COMPLETADA`. Si el repartidor rechaza, vuelve de `ASIGNADA` a `PENDIENTE_ASIGNACION` y se reasigna. Desde `RETIRADA` también se puede completar directamente. Cada cambio queda registrado en el historial con el estado anterior, el nuevo, la fecha y el actor.
- Un repartidor solo puede tener una entrega activa. Mientras la tiene queda `OCUPADO` y al completarla vuelve a `DISPONIBLE`.
- Cada repartidor trabaja con un solo vehículo activo, que no puede cambiar ni quitar durante una entrega. Las patentes no se repiten.
- Si una entrega indica zona, solo se asigna a repartidores de esa zona.
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

Son 197 tests. Incluyen tests de entidades, repositorios, services (con la base H2 real), validación de requests, contratos JSON, manejo de errores, controllers de punta a punta con MockMvc, la configuración de OpenAPI y la carga de datos de demo.

## Documentación

- `doc/Grupo08_Primera_Entrega_Repartidores_y_Logistica.docx`: especificación funcional y técnica del módulo (primera entrega).
- `doc/openapi.yaml`: contrato OpenAPI exportado de la API.

## Pendiente

- Cancelación de entregas: la inicia el módulo Pedidos y no es un caso de uso de este módulo.
- Control de roles (administrador / repartidor): hoy solo se controla que el usuario sea el titular o el repartidor asignado.
