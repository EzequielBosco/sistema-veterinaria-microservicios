# Huellitas Perdidas - Sistema Veterinario (Microservicios)

Migración del monolito `sistema-veterinaria-Bosco` a una arquitectura de microservicios.
Proyecto Maven multi-módulo, Spring Boot 4.1.1, Spring Cloud 2025.1.3, Java 21, MySQL.

## Módulos

### Infraestructura

| Módulo | Puerto | Tecnología | Rol |
|---|---|---|---|
| `config-server` | 8888 | Spring Cloud Config | Configuración centralizada para todos los microservicios |
| `eureka-server` | 8761 | Spring Cloud Netflix Eureka | Service Discovery: registro y localización de servicios |
| `api-gateway` | 8080 | Spring Cloud Gateway (WebFlux) | **Único punto de entrada** para los clientes externos: enruta a cada microservicio vía Eureka |

### Microservicios

| Módulo | Puerto | Base de datos | Entidades |
|---|---|---|---|
| `ms-duenio` | 8081 | `duenio_db` | Duenio |
| `ms-mascota` | 8082 | `mascota_db` | Mascota |
| `ms-veterinario` | 8083 | `veterinario_db` | Veterinario |
| `ms-turno` | 8084 | `turno_db` | Turno, Participacion |

Cada microservicio es dueño de sus datos y tiene su propia base de datos: no hay JOINs ni foreign keys
entre bases de distintos servicios. Las referencias a otros contextos se guardan por ID (`Long`).

## Cómo ejecutar

Requisitos: Java 21 y MySQL corriendo en `localhost:3306` (usuario/clave `root`/`root`).
Las bases se crean solas (`createDatabaseIfNotExist=true`) y Hibernate genera las tablas.

```bash
# Compilar y testear todos los módulos desde la raíz
./mvnw clean package
```

> Si Maven falla al descargar dependencias con `PKIX path building failed` (un antivirus o proxy que inspecciona
> HTTPS), en Windows se resuelve usando el almacén de certificados del sistema:
> `$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"` (PowerShell) antes de `.\mvnw.cmd`.

### Orden de arranque (obligatorio)

Cada uno en su propia terminal, esperando que el anterior esté arriba:

```bash
# 1. Config Server: tiene que estar UP antes que cualquier microservicio
./mvnw spring-boot:run -pl config-server

# 2. Eureka Server: tiene que estar UP antes que los microservicios
./mvnw spring-boot:run -pl eureka-server

# 3. Microservicios, en cualquier orden
./mvnw spring-boot:run -pl ms-duenio
./mvnw spring-boot:run -pl ms-mascota
./mvnw spring-boot:run -pl ms-veterinario
./mvnw spring-boot:run -pl ms-turno

# 4. API Gateway
./mvnw spring-boot:run -pl api-gateway
```

- Si un microservicio arranca **antes que Config Server**, no obtiene su configuración (datasource, JPA,
  Eureka) y falla al arrancar por falta de datasource. El gateway, en cambio, **no arranca** sin Config Server
  (import sin `optional:`): sin su config no tendría rutas.
- Si arranca **antes que Eureka**, no puede registrarse, pero reintenta automáticamente y aparece en el dashboard cuando Eureka levanta.
- El gateway y los servicios que usan Feign refrescan su copia del registro de Eureka cada 30 segundos: justo
  después de levantar un servicio, las llamadas a él pueden fallar durante ese lapso.

### Verificación

| Qué | URL |
|---|---|
| Dashboard de Eureka (deben aparecer los 4 servicios y `API-GATEWAY`) | `http://localhost:8761` |
| Rutas del gateway | `http://localhost:8080/actuator/gateway/routes` |
| Config de un servicio servida por Config Server | `http://localhost:8888/ms-duenio/default` |
| Swagger de cada servicio | `http://localhost:{puerto}/swagger-ui/index.html` |
| Estado del servicio (incluye BD y Eureka) | `http://localhost:{puerto}/actuator/health` |
| Información del servicio | `http://localhost:{puerto}/actuator/info` |

## Config Server

La configuración se centraliza en `config-server/src/main/resources/config-repo/`. Config Server corre con el
perfil `native`, que sirve los archivos desde el classpath en lugar de un repositorio Git:

| Archivo | Contenido |
|---|---|
| `application.yml` | Config compartida por todos: URL de Eureka, JPA (`ddl-auto`, `show-sql`) y Actuator |
| `ms-<servicio>.yml` | Config propia de cada servicio: datasource e `info.app` (tiene prioridad sobre la compartida) |

Cada microservicio solo tiene localmente su nombre, su puerto y el import del Config Server:

```yaml
server:
  port: 8081
spring:
  application:
    name: ms-duenio            # define qué archivo pide (ms-duenio.yml) y su nombre en Eureka
  config:
    import: "optional:configserver:http://localhost:8888"
```

Para pasar a un repositorio Git, se quita el perfil `native` en `config-server/src/main/resources/application.yml`
y se configura `spring.cloud.config.server.git.uri` apuntando al repo con los mismos archivos.

## Actuator

Todos los microservicios exponen `health`, `info`, `metrics`, `env`, `beans` y `mappings` bajo `/actuator`
(configurado en el `application.yml` compartido del Config Server). `health` muestra el detalle completo
(`show-details: always`) e `info` devuelve los datos de `info.app` de cada servicio.

## API Gateway

A partir del Sprint 12 **los clientes externos solo usan `http://localhost:8080`**: los puertos 8081-8084 son un
detalle interno. El gateway recibe cada request y, según el path, la reenvía al microservicio que corresponde,
resolviendo su ubicación en Eureka (`lb://`):

| Path externo (:8080) | Microservicio destino | Puerto interno |
|---|---|---|
| `/api/duenios/**` | `ms-duenio` | 8081 |
| `/api/mascotas/**` | `ms-mascota` | 8082 |
| `/api/veterinarios/**` | `ms-veterinario` | 8083 |
| `/api/turnos/**` | `ms-turno` | 8084 |
| `/actuator/**` | el propio `api-gateway` | 8080 |

```
Cliente ──▶ api-gateway :8080 ──LoggingFilter──▶ ruta por Path ──lb://ms-x (Eureka)──▶ ms-x :808x
```

- **Rutas, CORS y Actuator** del gateway están en `config-repo/api-gateway.yml`; localmente solo tiene puerto,
  nombre e import del Config Server. En Spring Cloud Gateway 5 las propiedades van bajo
  `spring.cloud.gateway.server.webflux.*`.
- **Logging centralizado:** `LoggingFilter` (un `GlobalFilter` con orden -1) registra cada request y su
  respuesta: `[GATEWAY] --> POST /api/turnos` / `[GATEWAY] <-- POST /api/turnos 201 597ms`.
- **CORS:** permite cualquier origen (solo desarrollo) con `GET, POST, PUT, PATCH, DELETE, OPTIONS`. `PATCH`.
- **Discovery locator:** además de las 4 rutas, el gateway crea una ruta automática por cada servicio de Eureka
  con su nombre como prefijo (`/ms-duenio/api/duenios/1`).
- **Actuator:** `/actuator/gateway/routes` lista las rutas activas. El endpoint viene deshabilitado por defecto
  (`management.endpoint.gateway.access: none`); se habilitó en modo `read-only`.
- El gateway es **reactivo** (WebFlux, no Servlet): no debe tener `spring-boot-starter-webmvc`.
- Los errores de negocio de los microservicios (400, 404, 409, 422 con su `ErrorResponse`) pasan intactos.
- La comunicación **interna** (Feign entre servicios) no pasa por el gateway: va directo vía Eureka.

## Comunicación entre microservicios (OpenFeign)

Las referencias por ID a otros servicios se validan con clientes **OpenFeign** que llaman por el nombre
registrado en Eureka (`http://ms-mascota/...`), sin IP ni puerto hardcodeados: Spring Cloud LoadBalancer
resuelve el nombre a la instancia real y, si hay varias, las rota.

| Quién valida | Contra quién | Qué valida | Cuándo |
|---|---|---|---|
| ms-turno | ms-mascota | Que exista `mascotaId` | Al crear y al actualizar un turno |
| ms-turno | ms-veterinario | Que exista cada `veterinarioId` del turno | Al crear y al actualizar un turno |
| ms-mascota | ms-duenio | Que exista `duenioId` | Al crear una mascota y al cambiarla de dueño |

En todos los casos primero se validan las reglas locales (datos obligatorios, cupo de mascotas, duración,
rol `PRINCIPAL`) y recién después se consulta al otro servicio: un request inválido no genera llamadas HTTP.

```
POST /api/turnos ──▶ TurnoController ──▶ TurnoServiceImpl
                                           ├─ 1. reglas locales (duración, PRINCIPAL, ...) → 400
                                           ├─ 2. MascotaServicePort.existsById ──Feign──▶ ms-mascota     → 404
                                           ├─ 3. VeterinarioServicePort.existsById ─Feign─▶ ms-veterinario → 404
                                           ├─ 4. superposición horaria                   → 409
                                           └─ 5. TurnoRepositoryPort.guardar ──▶ turno_db
```

| Situación | Respuesta |
|---|---|
| La mascota de un turno no existe en ms-mascota | 404 `No existe una mascota con id X` |
| Algún veterinario de un turno no existe en ms-veterinario | 404 `No existe un veterinario con id X` |
| El dueño de una mascota no existe en ms-duenio | 404 `No existe un duenio con id X` |
| El servicio consultado está caído o con timeout (después de 3 intentos) | 503 `El servicio de mascotas no esta disponible: ...` |
| El circuito hacia ese servicio está abierto | 503 inmediato `... no esta disponible: circuito abierto, ...` |

- Los clientes Feign, sus DTOs de consumo y los adaptadores viven en `adapter/out/feign/` del servicio que
  llama. El dominio solo conoce puertos con un `boolean existsById` (`MascotaServicePort`,
  `VeterinarioServicePort`, `DuenioServicePort`): el adaptador traduce el 404 remoto a `false` y no sabe nada
  de HTTP.
- Los DTOs de consumo (`MascotaResponse`, `VeterinarioResponse`, `DuenioResponse`) son clases **propias** del
  servicio que llama, con solo los campos que necesita: los microservicios no comparten código.
- Timeouts (`connect-timeout` 3 s, `read-timeout` 5 s), compresión y `logger-level` de Feign están en el
  `config-repo/application.yml` compartido. El nivel de log de cada paquete `adapter.out.feign` está en el
  `ms-<servicio>.yml` correspondiente. Cada llamada queda en el log del servicio que la hace:
  `[MascotaFeignClient#findById] ---> GET http://ms-mascota/api/mascotas/13`.
- Dependencias en tiempo de ejecución: para crear o actualizar turnos tienen que estar levantados
  ms-mascota y ms-veterinario; para crear una mascota o cambiarla de dueño, ms-duenio. Las consultas (GET),
  los eliminar y las ediciones que no cambian la referencia no dependen de otros servicios.

## Resiliencia (Resilience4j)

Cada llamada Feign está protegida con **Retry** y **Circuit Breaker** en su adaptador de salida, para que la caída
de un servicio no se propague en cascada: el cliente recibe un 503 claro en lugar de un error 500 o una espera
indefinida.

| Adaptador | Servicio protegido | Instancias (CB y Retry) |
|---|---|---|
| `MascotaFeignAdapter` (ms-turno) | ms-mascota | `mascotaService` |
| `VeterinarioFeignAdapter` (ms-turno) | ms-veterinario | `veterinarioService` |
| `DuenioFeignAdapter` (ms-mascota) | ms-duenio | `duenioService` |

```java
@Retry(name = "mascotaService", fallbackMethod = "fallbackMascota")   // exterior: reintenta y aplica el fallback
@CircuitBreaker(name = "mascotaService")                              // interior: cuenta cada intento
public boolean existsById(Long id) { ... }

private boolean fallbackMascota(Long id, Throwable ex) {              // mismos parámetros + Throwable
    throw new ServicioNoDisponibleException("El servicio de mascotas no esta disponible: ...");  // → 503
}
```

- **Retry:** hasta 3 intentos con espera exponencial (1 s, 2 s). No reintenta si el circuito está abierto
  (`ignore-exceptions: CallNotPermittedException`), así la respuesta con el circuito abierto es inmediata.
- **Circuit Breaker:** sobre las últimas 10 llamadas, con un mínimo de 5, si fallan el 50 % o más se abre
  (OPEN) y durante 10 s rechaza las llamadas sin tocar el servicio caído. Después pasa solo a HALF_OPEN, deja
  pasar 3 llamadas de prueba y, si salen bien, vuelve a CLOSED.
- **El 404 no es un fallo:** el adaptador lo resuelve como "no existe", así que no se reintenta ni abre el circuito.
- **Dónde se configura:** los parámetros están una sola vez en `configs.default` del `config-repo/application.yml`
  compartido; cada servicio declara sus instancias en su `ms-<servicio>.yml` con `base-config: default`. El
  `name` de las anotaciones tiene que coincidir con la instancia: si no, se usan los valores por defecto sin
  ningún error.

Comportamiento medido con ms-mascota apagado (`POST /api/turnos` repetido):

| Request | Qué pasa | Tiempo |
|---|---|---|
| 1 | 3 intentos fallidos con esperas de 1 s y 2 s; el circuito sigue CLOSED | ~9 s |
| 2 | Al 5.º fallo (80 %) el circuito se abre; el 3.er intento ya no se hace | ~3 s |
| 3 en adelante | Circuito OPEN: 503 inmediato sin llamar a ms-mascota | ~0,02 s |

**Monitoreo** (en ms-turno y ms-mascota):

| Endpoint | Qué muestra |
|---|---|
| `/actuator/health` | Componente `circuitBreakers` con el estado de cada circuito (`CLOSED`/`OPEN`/`HALF_OPEN`) |
| `/actuator/circuitbreakers` | Métricas de cada circuito (tasa de fallos, llamadas, rechazadas) |
| `/actuator/circuitbreakerevents/{nombre}` | Historial: éxitos, errores, llamadas rechazadas y transiciones de estado |

El estado general del servicio sigue `UP` aunque un circuito esté abierto: el caído es el otro servicio. El
componente de health lo aporta `CircuitBreakersHealthIndicator` de cada servicio, porque el indicador que trae
`resilience4j-spring-boot3` no se registra con Spring Boot 4 (busca la clase `HealthIndicator` de Boot 3).

## Endpoints

### ms-duenio (`/api/duenios`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/duenios` | Listar todos |
| GET | `/api/duenios/{id}` | Buscar por ID |
| GET | `/api/duenios/cedula/{cedula}` | Buscar por cédula/DNI |
| GET | `/api/duenios/email/{email}` | Buscar por email |
| GET | `/api/duenios/search?nombre=&apellido=` | Buscar por nombre y apellido |
| POST | `/api/duenios` | Crear (409 si email o cédula duplicados) |
| PUT | `/api/duenios/{id}` | Actualizar |
| DELETE | `/api/duenios/{id}` | Eliminar |

### ms-mascota (`/api/mascotas`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/mascotas` | Listar todas |
| GET | `/api/mascotas/{id}` | Buscar por ID |
| GET | `/api/mascotas/duenio/{duenioId}` | Mascotas de un dueño |
| POST | `/api/mascotas` | Crear (`duenioId` obligatorio; 422 si el dueño ya tiene 5 mascotas; 404 si el dueño no existe en ms-duenio; 503 si ms-duenio no responde) |
| PUT | `/api/mascotas/{id}` | Actualizar (si cambia de dueño valida el cupo y que el nuevo dueño exista) |
| DELETE | `/api/mascotas/{id}` | Eliminar |

### ms-veterinario (`/api/veterinarios`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/veterinarios` | Listar todos |
| GET | `/api/veterinarios/{id}` | Buscar por ID |
| GET | `/api/veterinarios/especialidad/{especialidad}` | Veterinarios de una especialidad (sin distinguir mayúsculas) |
| POST | `/api/veterinarios` | Crear (409 si matrícula o email duplicados) |
| PUT | `/api/veterinarios/{id}` | Actualizar |
| DELETE | `/api/veterinarios/{id}` | Eliminar |

### ms-turno (`/api/turnos`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/turnos` | Listar todos |
| GET | `/api/turnos?veterinarioId=&fecha=` | Agenda de un veterinario en una fecha (ambos parámetros) |
| GET | `/api/turnos/{id}` | Buscar por ID |
| GET | `/api/turnos/{id}/veterinarios` | Veterinarios del turno con su rol |
| GET | `/api/turnos/veterinario/{veterinarioId}` | Turnos de un veterinario con su rol |
| GET | `/api/turnos/mascota/{mascotaId}` | Historial de turnos de una mascota |
| POST | `/api/turnos` | Crear (404 si la mascota o algún veterinario no existen; 503 si esos servicios no responden) |
| PUT | `/api/turnos/{id}` | Actualizar (reemplaza los veterinarios asignados; mismas validaciones que el POST) |
| PATCH | `/api/turnos/{id}/cancelar` | Cancelar (422 si ya está `CANCELADO`, `ATENDIDO` o `AUSENTE`) |
| DELETE | `/api/turnos/{id}` | Eliminar |

Reglas de turnos: fecha y hora futuras, duración mínima de 10 minutos, al menos un veterinario,
exactamente uno con rol `PRINCIPAL`, sin veterinarios repetidos y sin superposición horaria
para un mismo veterinario (409). Los turnos cancelados no cuentan para la superposición:
cancelar un turno libera el horario de sus veterinarios.

## Arquitectura

Cada microservicio sigue **Arquitectura Hexagonal (Ports & Adapters)**:

```
ms-<servicio>/src/main/java/com/veterinaria/<servicio>/
├── domain/                              ZONA INTERNA - reglas y contratos del negocio
│   ├── model/                           entidades y enums
│   ├── port/in/<X>Service.java          puerto de entrada (casos de uso)
│   ├── port/out/<X>RepositoryPort.java  puerto de salida (persistencia)
│   └── exception/                       excepciones de negocio
├── application/                         ZONA MEDIA
│   └── <X>ServiceImpl.java              implementa el puerto de entrada usando los de salida
└── adapter/                             ZONA EXTERNA
    ├── in/web/                          adaptador de entrada REST
    │   ├── <X>Controller.java
    │   ├── GlobalExceptionHandler.java, ErrorResponse.java
    │   └── dto/                         Request, Response y Mapper (manual)
    └── out/
        ├── persistence/
        │   └── <X>Repository.java       Spring Data JPA, implementa <X>RepositoryPort
        └── feign/                       (ms-turno y ms-mascota) clientes HTTP hacia otros microservicios
            ├── <Y>FeignClient.java      interfaz @FeignClient(name = "ms-<y>")
            ├── <Y>Response.java         DTO de consumo (copia reducida del JSON remoto)
            └── <Y>FeignAdapter.java     implementa <Y>ServicePort (puerto de salida del dominio)
```

**Regla de dependencias:** las dependencias siempre apuntan hacia adentro.

- `domain` no depende de `application` ni de `adapter`: los puertos son interfaces Java puras,
  sin Spring.
- `application` solo conoce `domain` (no importa nada de `adapter.*`).
- Los adaptadores conocen el dominio y lo usan a través de los puertos. Que un adaptador dependa
  del dominio está permitido; lo prohibido es la dirección inversa.

```
HTTP ──▶ <X>Controller ──▶ <X>Service (port in)
                                 ▲
                          <X>ServiceImpl
                                 │
                      <X>RepositoryPort (port out)
                                 ▲
                         <X>Repository (JPA) ──▶ <servicio>_db
```

### Decisiones de modelado respecto del monolito

- **`Persona` se aplanó:** era un `@MappedSuperclass` (sin tabla propia) del que heredaban `Duenio` y
  `Veterinario`. Como los microservicios no comparten clases de dominio, sus campos (nombre, apellido,
  teléfono, email) se copian en cada entidad.
- **Relaciones entre servicios por ID:** `Mascota.duenioId`, `Turno.mascotaId` y
  `Participacion.veterinarioId` son `Long`, sin `@ManyToOne`. Se pierden las listas inversas
  (`Duenio.mascotas`, `Mascota.turnos`, `Veterinario.participaciones`) y los borrados en cascada entre servicios.
- **`Participacion` vive en ms-turno:** es la entidad intermedia del muchos a muchos Turno ↔ Veterinario
  y su ciclo de vida depende del Turno. Así la regla de superposición se sigue validando localmente.
- **Endpoints reubicados:** `GET /api/duenios/{id}/mascotas` pasa a `GET /api/mascotas/duenio/{duenioId}`,
  `POST /api/duenios/{id}/mascotas` pasa a `POST /api/mascotas` con `duenioId` en el body y
  `GET /api/veterinarios/{id}/turnos` pasa a `GET /api/turnos/veterinario/{veterinarioId}`.