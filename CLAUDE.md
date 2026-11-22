# DeliveryGo — Repartidores y Logística

## 1. Contexto del proyecto

Proyecto académico de Aplicaciones Interactivas de UADE.

Implementación del módulo Repartidores y Logística del sistema DeliveryGo.

La especificación funcional y arquitectónica se encuentra en el documento de la primera entrega. Debe utilizarse como fuente principal de requisitos.

## 2. Tecnologías

- Java 25.
- Spring Boot 3.
- Maven.
- Spring Data JPA / Hibernate.
- H2 Database.
- API REST con JSON.
- springdoc-openapi para la documentación de la API.

Respetar las versiones y dependencias ya definidas en pom.xml.

## 3. Alcance

Casos de uso CORE (implementados):

- CU-01: Registrar repartidor.
- CU-02: Cambiar disponibilidad del repartidor.
- CU-03: Asignar repartidor a una entrega.
- CU-04: Registrar retiro del pedido.
- CU-05: Confirmar entrega del pedido.

Casos de uso de evolución (implementados el 22/11/2026, rama `examen`):

- CU-06: Aceptar o rechazar la asignación.
- CU-07: Informar pedido en camino.
- CU-08: Consultar seguimiento de la entrega (y datos del repartidor).
- CU-09: Administrar vehículos del repartidor.
- CU-10: Administrar zonas de operación.

No agregar funcionalidades fuera del alcance sin justificar su necesidad.

## 4. Arquitectura

Respetar la separación de responsabilidades:

- Controller: recibe solicitudes HTTP, valida DTO y devuelve respuestas.
- Service: implementa los casos de uso, reglas de negocio y transacciones.
- Repository: realiza el acceso a la base de datos propia.
- Domain / Entity: representa el dominio y protege sus invariantes.
- DTO: define los contratos de entrada y salida.
- Mapper: transforma entidades y DTO.
- Client: encapsula las integraciones HTTP.
- Exception: centraliza las excepciones y su traducción a respuestas HTTP.
- Config: contiene la configuración de infraestructura.

Los controllers no deben contener lógica de negocio.

Las entidades no deben exponerse directamente en la API.

## 5. Reglas de negocio

Respetar las RN-01 a RN-13 definidas en la especificación.

En particular:

- DNI único y un único repartidor por usuario.
- Solo usuarios elegibles pueden registrarse como repartidores.
- Solo repartidores ACTIVOS y DISPONIBLES pueden recibir asignaciones.
- Un repartidor no puede tener más de una entrega activa.
- Las transiciones de estado deben ser válidas.
- No se puede completar una entrega que no haya sido retirada.
- Solo el repartidor asignado puede registrar el retiro y confirmar la entrega.
- Cada transición debe generar su historial.
- La entrega se completa únicamente con un código de confirmación válido.

No modificar reglas de negocio sin explicar la discrepancia y solicitar autorización.

## 6. Persistencia

- Usar H2 Database y JPA.
- Persistir los enums como texto mediante EnumType.STRING.
- Respetar las relaciones y decisiones de modelado de la especificación.
- Pedido, cliente y comercio pertenecen a otros módulos: guardar sus identificadores y los datos operativos necesarios, sin crear relaciones JPA con sus tablas.
- Aplicar restricciones de integridad en la base de datos cuando corresponda.
- Respetar la estrategia de concurrencia definida en la especificación.

## 7. Contratos REST

Respetar los endpoints, métodos HTTP, DTO, códigos de respuesta y códigos de error definidos en el documento.

No incluir el código de confirmación en EntregaResponse.

No aceptar repartidorId en las operaciones de retiro y confirmación para determinar la identidad del repartidor. La identidad debe provenir del mecanismo de autenticación definido para el proyecto.

## 8. Forma de trabajar

- Trabajar en cambios pequeños e incrementales.
- Antes de modificar archivos, explicar brevemente qué se va a cambiar.
- No modificar archivos ajenos a la tarea sin autorización.
- No reemplazar configuraciones existentes sin explicar el motivo.
- No agregar dependencias innecesarias.
- No inventar contratos de otros módulos.
- Si falta información, preguntar antes de tomar una decisión importante.
- No afirmar que una funcionalidad está probada si no se ejecutaron las pruebas correspondientes.
- Explicar las decisiones técnicas de manera que un estudiante pueda comprenderlas y defenderlas.
- No agregar `Co-Authored-By` ni otra atribución a Claude en los commits ni en los pull requests.

## 9. Validación obligatoria

Después de cada cambio:

1. Ejecutar los tests pertinentes.
2. Compilar el proyecto.
3. Informar los archivos modificados.
4. Resumir las reglas de negocio implementadas.
5. Indicar qué pruebas se ejecutaron y sus resultados.
6. Señalar las limitaciones o funcionalidades pendientes.

Utilizar el Maven Wrapper del proyecto cuando esté disponible.

## 10. Decisiones tomadas

Estas decisiones ya fueron acordadas. No revertirlas sin consultar.

### Referencias

- Ramas: `examen` es la versión que se defiende ante el profesor (CU-01 a CU-10, mocks y `DemoDataLoader` para la demostración). `main` quedó como demostración de la integración con los otros módulos (pedidoId UUID, JWT, CORS, `/status`, clientes HTTP). Las dos ramas no se mergean entre sí.

- Especificación: `Grupo08_Primera_Entrega_Repartidores_y_Logistica.docx` (fuera del repositorio, en `Desktop\DESAROLLO DE APPS`). Es la versión vigente; el PDF del escritorio es anterior.
- Ejemplo de clase del profesor: carpeta `deliverygo-persistence 2` (fuera del repositorio). Muestra cómo espera la cátedra que se trabaje.
- Ante una diferencia entre la especificación y el ejemplo de clase, señalarla y consultar. Por defecto se sigue el ejemplo de clase.

### Convenciones del proyecto

- IDs `Long` con `GenerationType.IDENTITY`, como en el ejemplo de clase. No se usa UUID, aunque el diagrama de clases lo indique. Los IDs de otros módulos (`pedidoId`, `clienteId`, `comercioId`, `usuarioId`) también son `Long`.
- Paquete base `ar.edu.uade.deliverygo.logistica`, con subpaquetes `model`, `dto`, `mapper`, `repository`, `service`, `excepcion`, `client`, `controller` y `config`. La conversión entidad → Response la hacen `RepartidorMapper` y `EntregaMapper` (`@Component`, método `toResponse`), como prevé la sección 12 de la especificación (03/11/2026; antes estaba en cada service como `convertirResponse`, al estilo del `convertirDTO` de clase). La creación de entidades a partir de un Request queda en el service, porque se combina con reglas (estado inicial, fecha de alta).
- DTO como clases con getters y setters, sin Lombok. Los requests tienen constructor vacío y setters; los responses tienen campos `final` y solo getters.
- DTO: regla única en todo el módulo, como la filmina de la cátedra ("Validación en la Frontera") y la sección 15 de la especificación: lo que entra es un `...Request` validado con `@Valid`; lo que sale es un `...Response`. Se probó un único `RepartidorDTO` (DM-13, 01/11/2026) y se revirtió el mismo día para tener una sola regla y `@Valid` en todos los endpoints. `RepartidorResponse` incluye `dni` y `telefono` (DTO-02 de la especificación no los incluía).
- Los requests no tienen campos que define el sistema (`id`, `estado`, `disponibilidad` en el alta, `fechaAlta`) ni datos de identidad (`repartidorId`/`usuarioId` en CU-02 y CU-04 a CU-07, CU-09 y CU-10, RN-10). `EntregaResponse` no tiene el código de confirmación (RN-13).
- Limpieza (03/11/2026): se borraron `VehiculoService` y `HistorialEstadoEntregaService` (vacíos), `HistorialEstadoEntregaRepository` y `VehiculoRepository` (sin uso y fuera de la especificación). Vehículos e historial se guardan siempre en cascada desde `Repartidor` y `Entrega`.
- Validación de entrada (RN-01) con Bean Validation: anotaciones en los requests y `@Valid` en el controller. Las reglas que dependen de otro campo o de una consulta (RN-02, RN-03, RN-04, RN-05, RN-07, RN-10) van en el service o en la entidad. Las restricciones de la base (`nullable`, `unique`) se mantienen como defensa en profundidad.
- RN-03 / INT-01: interfaz `client.UsuariosClient` implementada por `UsuariosClientMock` (usuarios 1 a 5 elegibles, 6 inactivo, 7 rol CLIENTE). Cuando exista el módulo Usuarios se agrega una implementación HTTP sin tocar el service.
- INT-03 y INT-05: `PedidosClient` + `PedidosClientMock` (pedidos 100 a 105 CONFIRMADOS, 106 PENDIENTE, el resto no existe) `NotificacionesClient` + `NotificacionesClientMock` (avisos de asignación, retiro, en camino y entrega) y `AnaliticaClient` + `AnaliticaClientMock` (INT-06, evento DELIVERY_COMPLETED con los tiempos); los mocks de avisos solo loguean. `PedidosClient.informarEstadoEntrega` cubre INT-04 (endpoint a acordar con Pedidos). Los avisos a Pedidos y Notificaciones no revierten la operación si fallan (DM-12): `EntregaService.avisarSinRevertir` los captura y los loguea. DM-12 decía reintentar hasta 3 veces; no se implementó el reintento.
- Historial (RN-12): cuando el cambio lo hace el sistema o el módulo Pedidos se registra `EntregaService.ACTOR_SISTEMA = 0L`. El primer registro de cada entrega tiene `estadoAnterior = null`.
- DM-09 simplificado: solo `@Version` en `Repartidor`, sin reintento automático. Si dos asignaciones toman al mismo repartidor, la segunda falla al guardar (`ObjectOptimisticLockingFailureException`) y el `GlobalExceptionHandler` responde 409 `CONCURRENT_UPDATE` (código no listado en la sección 17; CU-03 A6 pide 409 sin nombrarlo). Pedidos puede volver a pedir la asignación.
- Código de confirmación: 4 dígitos con `SecureRandom`, generado en `EntregaService` y pasado a `Entrega.asignar`.
- Java 25 y Spring Boot 3.5.16. La sección 11 de la especificación menciona Java 21 y PostgreSQL; prevalece este archivo.
- H2 en memoria (`jdbc:h2:mem:logistica`), `ddl-auto=create-drop`, `open-in-view=false` y consola en `/h2-console`.

### Dominio

- Las reglas viven en las entidades:
  - `Repartidor`: `cambiarDisponibilidad`, `ocupar`, `liberar`, `vehiculoActivo`, `buscarVehiculo`, `activarVehiculo` (no durante una entrega), `quitarVehiculo` (no el activo) y `asignarZona`.
  - `Entrega`: `asignar(repartidor, codigo)`, `aceptar`, `rechazar`, `registrarRetiro`, `informarEnCamino`, `completar(codigo)` y `validarTransicion` (privado).
- Máquina de estados (RN-08): `PENDIENTE_ASIGNACION → ASIGNADA`; `ASIGNADA → RETIRADA` (CU-04) o `→ PENDIENTE_ASIGNACION` (rechazo, CU-06); `RETIRADA → EN_CAMINO` (CU-07) o `→ COMPLETADA`; `EN_CAMINO → COMPLETADA`. La cancelación no está: la inicia Pedidos y no es un CU del módulo.
- CU-06: aceptar no cambia el estado (sigue ASIGNADA) pero queda en el historial. Rechazar libera al repartidor, borra el código y la fecha de asignación, y reasigna enseguida con el criterio de CU-03 y un código nuevo; el reemplazo se busca antes de liberar a quien rechaza, para que no le vuelva la misma entrega. Si no hay otro, queda PENDIENTE_ASIGNACION.
- CU-07: solo avisa a Notificaciones; a Pedidos no, porque según la máquina de estados ese paso no cambia el pedido.
- CU-08: `GET /deliveries/{id}` devuelve la entrega con el historial ordenado (`@OrderBy("id ASC")`), sin el código. No pide identificación (los roles no se controlan).
- CU-09: un vehículo nuevo queda inactivo hasta activarlo; siempre hay uno solo activo (REL-01). La patente no se repite (REP-11, 409 `VEHICLE_PLATE_DUPLICATED`, también en CU-01).
- CU-10: entidad `Zona` (nombre único). El repartidor tiene una zona opcional (`@ManyToOne`) y la entrega un `zonaId` opcional que manda Pedidos. Si la entrega trae zona, solo se eligen repartidores de esa zona (REP-10); si no, cualquiera. Si un administrador indica el `repartidorId`, no se filtra por zona.
- El código de confirmación lo genera el service (SRV-03) y se pasa a `Entrega.asignar`.
- El historial (RN-12) lo registra el service (SRV-06), no la entidad. El actor es el repartidor en CU-04 a CU-07 y `ACTOR_SISTEMA` en las asignaciones (también la reasignación de CU-06).
- `Entrega.historial` usa `cascade = ALL` sin `orphanRemoval`, porque el historial no se borra nunca. `Repartidor.vehiculos` usa `cascade = ALL` y `orphanRemoval = true`.
- REP-04 depende de que H2 ordene los `NULL` primero. Si se migra a otra base de datos, revisarlo.

### Errores

- Los errores que puede provocar un usuario se lanzan como `ReglaNegocioException` con un `CodigoError`, que toma los códigos de la sección 17 de la especificación. Con los CU de evolución se agregaron `VEHICLE_NOT_FOUND` y `ZONE_NOT_FOUND` (404), `VEHICLE_PLATE_DUPLICATED`, `VEHICLE_IS_ACTIVE` y `ZONE_NAME_DUPLICATED` (409).
- Los errores de programación se lanzan como `IllegalStateException` o `IllegalArgumentException`.
- `ErrorResponse` usa los campos de la especificación: timestamp, status, code, message, path y correlationId. Los nombres del ejemplo de clase eran distintos (`error`, `mensaje`).
- Cada `CodigoError` tiene su `HttpStatus` (sección 17). `GlobalExceptionHandler` traduce `ReglaNegocioException`, errores de `@Valid`, JSON inválido, parámetros de tipo inválido y header faltante (400 `VALIDATION_ERROR`) y el conflicto de `@Version` (409 `CONCURRENT_UPDATE`). `correlationId` es un UUID nuevo por error, que también se escribe en el log. Los errores no previstos (500) y los propios de Spring (404 de URL inexistente, 405) los responde Spring con su formato.
- RN-10: la identidad del usuario llega en el header `X-Usuario-Id`, que simula el `usuarioId` que vendría en el JWT de Usuarios. No es seguro (cualquiera puede mandar otro id); es una simplificación mientras no exista el módulo Usuarios. Los roles (ADMIN, REPARTIDOR) no se controlan.

### Tests

- Se usa `spring-boot-starter-test`. Los tests se mantienen durante el desarrollo.
- `JAVA_HOME` no está definido en el sistema. Para usar el Maven Wrapper desde la terminal: `JAVA_HOME=C:\Users\NICO\.jdks\ms-25.0.4`.
- 197 tests en verde (22/11/2026). Los CU de evolución tienen tests en `EntregaServiceTest`, `RepartidorServiceTest`, `ZonaServiceTest`, `EntregaTest`, `RepartidorTest`, `RepartidorRepositoryTest` y en los tests de controllers (incluido `ZonaControllerTest`).
- `config.DemoDataLoader` (17/11/2026, ampliado el 22/11/2026), al estilo del ejemplo de clase: `@Configuration` + `@Bean CommandLineRunner`, con `@Profile("demo")`. Carga los datos usando los services (pasan por las reglas): repartidores de los usuarios 1 (MOTO), 2 (BICICLETA) y 3 (AUTO); entrega del pedido 100 ASIGNADA al 1 (CU-06 y CU-04), del 101 RETIRADA por el 2 (CU-07 y CU-05); el 3 DISPONIBLE, en la zona Centro y con una bicicleta inactiva (CU-09 y CU-10); zonas Centro y Palermo (Palermo sin repartidores). Imprime en el log los códigos de confirmación (solo para la demo; la API no los expone). Se arranca con `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=demo"`. Probado en `DemoDataLoaderTest`, con una base H2 aparte (`jdbc:h2:mem:demo`).

## 11. Plan y estado actual

Se trabaja de a un paso por vez, explicando cada cambio, con commits separados por tema. No se commitea sin pedido explícito.

- **Paso 1: repositorios y services, caso de uso por caso de uso.**
  - Repositorios REP-01 a REP-09: hechos.
  - CU-01 (`RepartidorService.registrarRepartidor`): hecho, con `RepartidorServiceTest` y `RequestsValidacionTest`.
  - CU-02 (`RepartidorService.cambiarDisponibilidad(id, dto, usuarioId)`): hecho, con tests en `RepartidorServiceTest`. El `usuarioId` lo pasará el controller desde la autenticación (RN-10); el mecanismo sigue pendiente.
  - CU-03 (`EntregaService.asignarRepartidor`): hecho, con `EntregaServiceTest`. Devuelve la entrega ASIGNADA (el controller responderá 201) o PENDIENTE_ASIGNACION si no hay repartidores (202).
  - CU-04 (`EntregaService.registrarRetiro(id, request, usuarioId)`): hecho, con tests en `EntregaServiceTest`. El actor del historial es el `usuarioId` del repartidor.
  - CU-05 (`EntregaService.confirmarEntrega(id, request, usuarioId)`): hecho, con tests en `EntregaServiceTest`. Libera al repartidor y avisa a Pedidos, Notificaciones y Analítica.
  - **Paso 1 terminado**: los services de los cinco CU CORE están hechos y probados.
- **Paso 2: controllers, DTO, errores e integraciones**, una vez comprobada la lógica.
  - DTO-01 a DTO-09: hechos, con anotaciones de Bean Validation en DTO-01, DTO-03 y DTO-09.
  - `GlobalExceptionHandler`: hecho, con `GlobalExceptionHandlerTest`.
  - Controllers: hechos. `RepartidorController` (API-01 POST /api/v1/couriers → 201, API-02 PATCH /api/v1/couriers/{id}/availability → 200) y `EntregaController` (API-03 POST /api/v1/deliveries/assign → 201 o 202, API-04 POST /{id}/pickup → 200 con body opcional, API-05 POST /{id}/complete → 200). Probados de punta a punta con MockMvc en `RepartidorControllerTest` y `EntregaControllerTest`.
  - springdoc: hecho. `springdoc-openapi-starter-webmvc-ui` 2.8.14 (la línea 2.8 es la de Spring Boot 3.x), `config.OpenApiConfig` con el título y la descripción, Swagger UI en `/swagger-ui.html` y contrato en `/v3/api-docs`. Los controllers llevan `@Tag`, `@Operation`, `@ApiResponse` (cada código HTTP con sus códigos de error y el esquema `ErrorResponse`) y `@Parameter`, sin mencionar las RN en las descripciones (03/11/2026). Probado en `OpenApiConfigTest`. El contrato exportado va en `doc/openapi.yaml` (pedido del profesor), junto con la primera entrega.
  - `DemoDataLoader`: hecho (perfil `demo`).
- **Paso 3: casos de uso de evolución (22/11/2026)**, con sus tests, anotaciones OpenAPI y `doc/openapi.yaml` actualizado.
  - CU-06: `POST /api/v1/deliveries/{id}/accept` → 200 y `POST /{id}/reject` → 200 (body opcional con `motivo`). `EntregaService.aceptarAsignacion` y `rechazarAsignacion`.
  - CU-07: `POST /api/v1/deliveries/{id}/on-the-way` → 200. `EntregaService.informarEnCamino`.
  - CU-08: `GET /api/v1/deliveries/{id}` → 200 (`SeguimientoEntregaResponse` con `HistorialEstadoResponse`) y `GET /api/v1/couriers/{id}` → 200. `EntregaService.consultarSeguimiento` y `RepartidorService.buscarPorId`.
  - CU-09: `GET` y `POST /api/v1/couriers/{id}/vehicles` (→ 200 / 201), `PATCH /{id}/vehicles/{vehiculoId}/activate` → 200 y `DELETE /{id}/vehicles/{vehiculoId}` → 204.
  - CU-10: `POST /api/v1/zones` → 201, `GET /api/v1/zones` → 200 (`ZonaController`, `ZonaService`, `ZonaMapper`) y `PUT /api/v1/couriers/{id}/zone` → 200. `AsignarEntregaRequest` acepta `zonaId` opcional.
  - Repositorios nuevos: REP-10 (disponible por zona), REP-11 (patente existente), REP-12 y REP-13 (`ZonaRepository`).
- **Paso 4: revisión de trazabilidad** entre casos de uso, reglas de negocio y código, para la defensa.

### Decisiones pendientes

- CU-01 A6: cuando exista el cliente HTTP real de Usuarios, traducir su falla a 503 `USERS_SERVICE_UNAVAILABLE` (el mock no falla nunca).

### Deuda técnica conocida

- `setEstado` y `setDisponibilidad` siguen siendo públicos en las entidades. Restringirlos al reescribir los services.
- Aceptar dos veces la misma entrega deja dos registros ASIGNADA → ASIGNADA en el historial (queda constancia de cada aceptación).
- La patente se compara distinguiendo mayúsculas (`AB123CD` y `ab123cd` se consideran distintas).
