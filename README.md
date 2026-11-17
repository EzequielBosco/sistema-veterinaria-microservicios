# Huellitas Perdidas - Sistema Veterinario (Microservicios)

Migración del monolito `sistema-veterinaria-Bosco` a una arquitectura de microservicios.
Proyecto Maven multi-módulo, Spring Boot 4.1.1, Java 21, MySQL.

## Microservicios

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

# Levantar un microservicio
./mvnw spring-boot:run -pl ms-duenio
./mvnw spring-boot:run -pl ms-mascota
./mvnw spring-boot:run -pl ms-veterinario
./mvnw spring-boot:run -pl ms-turno
```

Swagger de cada servicio: `http://localhost:{puerto}/swagger-ui/index.html`

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