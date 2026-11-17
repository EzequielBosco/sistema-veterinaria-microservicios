# Huellitas Perdidas - Sistema Veterinario (Microservicios)

Migración del monolito `sistema-veterinaria-Bosco` a una arquitectura de microservicios.
Proyecto Maven multi-módulo, Spring Boot 4.1.1, Spring Cloud 2025.1.3, Java 21, MySQL.

## Módulos

### Infraestructura

| Módulo | Puerto | Tecnología | Rol |
|---|---|---|---|
| `config-server` | 8888 | Spring Cloud Config | Configuración centralizada para todos los microservicios |
| `eureka-server` | 8761 | Spring Cloud Netflix Eureka | Service Discovery: registro y localización de servicios |

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
```

- Si un microservicio arranca **antes que Config Server**, no obtiene su configuración (datasource, JPA,
  Eureka) y falla al arrancar por falta de datasource.
- Si arranca **antes que Eureka**, no puede registrarse, pero reintenta automáticamente y aparece en el dashboard cuando Eureka levanta.

### Verificación

| Qué | URL |
|---|---|
| Dashboard de Eureka (deben aparecer los 4 servicios) | `http://localhost:8761` |
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
| POST | `/api/mascotas` | Crear (`duenioId` obligatorio; 422 si el dueño ya tiene 5 mascotas) |
| PUT | `/api/mascotas/{id}` | Actualizar (valida el cupo si cambia de dueño) |
| DELETE | `/api/mascotas/{id}` | Eliminar |

### ms-veterinario (`/api/veterinarios`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/veterinarios` | Listar todos |
| GET | `/api/veterinarios/{id}` | Buscar por ID |
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
| POST | `/api/turnos` | Crear |
| PUT | `/api/turnos/{id}` | Actualizar (reemplaza los veterinarios asignados) |
| DELETE | `/api/turnos/{id}` | Eliminar |

Reglas de turnos: fecha y hora futuras, duración mínima de 10 minutos, al menos un veterinario,
exactamente uno con rol `PRINCIPAL`, sin veterinarios repetidos y sin superposición horaria
para un mismo veterinario (409).

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
    └── out/persistence/
        └── <X>Repository.java           Spring Data JPA, implementa <X>RepositoryPort
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