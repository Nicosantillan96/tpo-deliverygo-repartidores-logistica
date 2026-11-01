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

Implementar primero los casos de uso CORE:

- CU-01: Registrar repartidor.
- CU-02: Cambiar disponibilidad del repartidor.
- CU-03: Asignar repartidor a una entrega.
- CU-04: Registrar retiro del pedido.
- CU-05: Confirmar entrega del pedido.

No implementar CU-06 a CU-10 hasta que se solicite expresamente.

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

- Especificación: `Grupo08_Primera_Entrega_Repartidores_y_Logistica.docx` (fuera del repositorio, en `Desktop\DESAROLLO DE APPS`). Es la versión vigente; el PDF del escritorio es anterior.
- Ejemplo de clase del profesor: carpeta `deliverygo-persistence 2` (fuera del repositorio). Muestra cómo espera la cátedra que se trabaje.
- Ante una diferencia entre la especificación y el ejemplo de clase, señalarla y consultar. Por defecto se sigue el ejemplo de clase.

### Convenciones del proyecto

- IDs `Long` con `GenerationType.IDENTITY`, como en el ejemplo de clase. No se usa UUID, aunque el diagrama de clases lo indique. Los IDs de otros módulos (`pedidoId`, `clienteId`, `comercioId`, `usuarioId`) también son `Long`.
- Paquete base `ar.edu.uade.deliverygo.logistica`, con subpaquetes `model`, `dto`, `mapper`, `repository`, `service`, `excepcion` y `client`. Los paquetes `controller` y `config` se agregarán en el paso 2.
- DTO como clases con getters y setters, sin Lombok. Los requests tienen constructor vacío y setters; los responses tienen campos `final` y solo getters.
- DTO: regla única en todo el módulo, como la filmina de la cátedra ("Validación en la Frontera") y la sección 15 de la especificación: lo que entra es un `...Request` validado con `@Valid`; lo que sale es un `...Response`. Se probó un único `RepartidorDTO` (DM-13, 01/11/2026) y se revirtió el mismo día para tener una sola regla y `@Valid` en todos los endpoints. `RepartidorResponse` incluye `dni` y `telefono` (DTO-02 de la especificación no los incluía).
- Los requests no tienen campos que define el sistema (`id`, `estado`, `disponibilidad` en el alta, `fechaAlta`) ni datos de identidad (`repartidorId`/`usuarioId` en CU-02, CU-04 y CU-05, RN-10). `EntregaResponse` no tiene el código de confirmación (RN-13).
- Validación de entrada (RN-01) con Bean Validation: anotaciones en los requests y `@Valid` en el controller. Las reglas que dependen de otro campo o de una consulta (RN-02, RN-03, RN-04, RN-05, RN-07, RN-10) van en el service o en la entidad. Las restricciones de la base (`nullable`, `unique`) se mantienen como defensa en profundidad.
- RN-03 / INT-01: interfaz `client.UsuariosClient` implementada por `UsuariosClientMock` (usuarios 1 a 5 elegibles, 6 inactivo, 7 rol CLIENTE). Cuando exista el módulo Usuarios se agrega una implementación HTTP sin tocar el service.
- INT-03 y INT-05: `PedidosClient` + `PedidosClientMock` (pedidos 100 a 105 CONFIRMADOS, 106 PENDIENTE, el resto no existe) y `NotificacionesClient` + `NotificacionesClientMock` (solo loguea). `PedidosClient.informarEstadoEntrega` cubre INT-04 (endpoint a acordar con Pedidos). Los avisos a Pedidos y Notificaciones no revierten la operación si fallan (DM-12): `EntregaService.avisarSinRevertir` los captura y los loguea. DM-12 decía reintentar hasta 3 veces; no se implementó el reintento.
- Historial (RN-12): cuando el cambio lo hace el sistema o el módulo Pedidos se registra `EntregaService.ACTOR_SISTEMA = 0L`. El primer registro de cada entrega tiene `estadoAnterior = null`.
- DM-09 simplificado: solo `@Version` en `Repartidor`, sin reintento automático. Si dos asignaciones toman al mismo repartidor, la segunda falla al guardar (`ObjectOptimisticLockingFailureException`) y el `GlobalExceptionHandler` deberá responder 409; Pedidos puede volver a pedir la asignación.
- Código de confirmación: 4 dígitos con `SecureRandom`, generado en `EntregaService` y pasado a `Entrega.asignar`.
- Java 25 y Spring Boot 3.5.16. La sección 11 de la especificación menciona Java 21 y PostgreSQL; prevalece este archivo.
- H2 en memoria (`jdbc:h2:mem:logistica`), `ddl-auto=create-drop`, `open-in-view=false` y consola en `/h2-console`.

### Dominio

- Las reglas viven en las entidades:
  - `Repartidor`: `cambiarDisponibilidad`, `ocupar`, `liberar` y `vehiculoActivo`.
  - `Entrega`: `asignar(repartidor, codigo)`, `registrarRetiro`, `completar(codigo)` y `validarTransicion` (privado).
- La máquina de estados solo contempla las transiciones de los CU CORE. Las de CU-06, CU-07 y la cancelación se agregan cuando se implementen.
- El código de confirmación lo genera el service (SRV-03) y se pasa a `Entrega.asignar`.
- El historial (RN-12) lo registra el service (SRV-06), no la entidad.
- `Entrega.historial` usa `cascade = ALL` sin `orphanRemoval`, porque el historial no se borra nunca. `Repartidor.vehiculos` usa `cascade = ALL` y `orphanRemoval = true`.
- REP-04 depende de que H2 ordene los `NULL` primero. Si se migra a otra base de datos, revisarlo.

### Errores

- Los errores que puede provocar un usuario se lanzan como `ReglaNegocioException` con un `CodigoError`, que toma los códigos de la sección 17 de la especificación.
- Los errores de programación se lanzan como `IllegalStateException` o `IllegalArgumentException`.
- `ErrorResponse` usa los campos de la especificación: timestamp, status, code, message, path y correlationId. Los nombres del ejemplo de clase eran distintos (`error`, `mensaje`).

### Tests

- Se usa `spring-boot-starter-test`. Los tests se mantienen durante el desarrollo.
- `JAVA_HOME` no está definido en el sistema. Para usar el Maven Wrapper desde la terminal: `JAVA_HOME=C:\Users\NICO\.jdks\ms-25.0.4`.
- Al final se agregará un `DemoDataLoader` al estilo del ejemplo de clase, con `@Profile("demo")`.

## 11. Plan y estado actual

Se trabaja de a un paso por vez, explicando cada cambio, con commits separados por tema. No se commitea sin pedido explícito.

- **Paso 1: repositorios y services, caso de uso por caso de uso.**
  - Repositorios REP-01 a REP-09: hechos.
  - CU-01 (`RepartidorService.registrarRepartidor`): hecho, con `RepartidorServiceTest` y `RequestsValidacionTest`.
  - CU-02 (`RepartidorService.cambiarDisponibilidad(id, dto, usuarioId)`): hecho, con tests en `RepartidorServiceTest`. El `usuarioId` lo pasará el controller desde la autenticación (RN-10); el mecanismo sigue pendiente.
  - CU-03 (`EntregaService.asignarRepartidor`): hecho, con `EntregaServiceTest`. Devuelve la entrega ASIGNADA (el controller responderá 201) o PENDIENTE_ASIGNACION si no hay repartidores (202).
  - CU-04 (`EntregaService.registrarRetiro(id, request, usuarioId)`): hecho, con tests en `EntregaServiceTest`. El actor del historial es el `usuarioId` del repartidor.
  - Service de CU-05: pendiente.
- **Paso 2: controllers, DTO, errores e integraciones**, una vez comprobada la lógica.
  - DTO-01 a DTO-09: hechos, con anotaciones de Bean Validation en DTO-01, DTO-03 y DTO-09.
  - Pendientes: mappers, `GlobalExceptionHandler`, controllers, clients y springdoc.
- **Paso 3: pruebas integrales y revisión de trazabilidad** entre casos de uso, reglas de negocio y código.

### Decisiones pendientes

- RN-10: mecanismo de autenticación (JWT o un mecanismo simplificado).
- Qué valor lleva `correlationId` en `ErrorResponse`.
- CU-01 A6: cuando exista el cliente HTTP real de Usuarios, traducir su falla a 503 `USERS_SERVICE_UNAVAILABLE` (el mock no falla nunca).
- `GlobalExceptionHandler` (paso 2): traducir `MethodArgumentNotValidException` de Bean Validation a 400 `VALIDATION_ERROR`.

### Deuda técnica conocida

- La columna `patente` es `unique`. Si se registra una patente repetida, la base rechaza el insert con un error genérico (500), porque la especificación no define un código para ese caso. Revisarlo con CU-09.
- `setEstado` y `setDisponibilidad` siguen siendo públicos en las entidades. Restringirlos al reescribir los services.
- `VehiculoRepository`, `HistorialEstadoEntregaRepository`, `VehiculoService` y `HistorialEstadoEntregaService` no figuran en la especificación. Revisarlos en el paso 3.