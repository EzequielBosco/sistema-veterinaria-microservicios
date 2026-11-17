# Huellitas Perdidas - Sistema Veterinario (Microservicios)

Migración del monolito `sistema-veterinaria-Bosco` a una arquitectura de microservicios.
Proyecto Maven multi-módulo, Spring Boot 4.1.1, Java 21, MySQL.

## Microservicios

| Módulo | Puerto | Base de datos | Estado |
|---|---|---|---|
| `ms-duenio` | 8081 | `duenio_db` | Sprint 9 |
| `ms-mascota` | 8082 | `mascota_db` | Sprint 10 |
| `ms-veterinario` | 8083 | `veterinario_db` | Sprint 10 |
| `ms-turno` | 8084 | `turno_db` | Sprint 10 |

Cada microservicio es dueño de sus datos y tiene su propia base de datos: no hay JOINs ni foreign keys
entre bases de distintos servicios. Las referencias a otros contextos se guardan por ID (`Long`).

## Cómo ejecutar

Requisitos: Java 21 y MySQL corriendo en `localhost:3306` (usuario/clave por defecto `root`/`root`,
se pueden cambiar con las variables `DB_USER` y `DB_PASS`). La base `duenio_db` se crea sola
(`createDatabaseIfNotExist=true`) y Hibernate genera la tabla `duenios`.

```bash
# Compilar y testear desde la raíz
./mvnw clean package -pl ms-duenio

# Levantar
./mvnw spring-boot:run -pl ms-duenio
```

Swagger: http://localhost:8081/swagger-ui/index.html

### Endpoints de ms-duenio (`/api/duenios`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/duenios` | Listar todos |
| GET | `/api/duenios/{id}` | Buscar por ID (404 si no existe) |
| GET | `/api/duenios/cedula/{cedula}` | Buscar por cédula/DNI |
| GET | `/api/duenios/email/{email}` | Buscar por email |
| GET | `/api/duenios/search?nombre=&apellido=` | Buscar por nombre y apellido |
| POST | `/api/duenios` | Crear (409 si email o cédula duplicados) |
| PUT | `/api/duenios/{id}` | Actualizar |
| DELETE | `/api/duenios/{id}` | Eliminar |

## Arquitectura

Cada microservicio sigue **Arquitectura Hexagonal (Ports & Adapters)**:

```
ms-duenio/src/main/java/com/veterinaria/duenio/
├── domain/                         ZONA INTERNA - no depende de Spring
│   ├── model/Duenio.java           entidad
│   ├── port/in/DuenioService.java          puerto de entrada (casos de uso)
│   ├── port/out/DuenioRepositoryPort.java  puerto de salida (persistencia)
│   └── exception/                  excepciones de negocio
├── application/                    ZONA MEDIA
│   └── DuenioServiceImpl.java      implementa DuenioService usando DuenioRepositoryPort
└── adapter/                        ZONA EXTERNA
    ├── in/web/                     adaptador de entrada REST
    │   ├── DuenioController.java
    │   ├── GlobalExceptionHandler.java, ErrorResponse.java
    │   └── dto/                    DuenioRequest, DuenioResponse, DuenioMapper (MapStruct)
    └── out/persistence/
        └── DuenioJpaRepository.java   implementa DuenioRepositoryPort con Spring Data JPA
```

**Regla de dependencias:** las dependencias siempre apuntan hacia adentro.

- `domain` no conoce a nadie: los puertos son interfaces Java puras.
- `application` solo conoce `domain` (no importa nada de `adapter.*`).
- Los adaptadores conocen el dominio a través de los puertos.

### ¿Por qué el controller no usa `DuenioServiceImpl`?

`DuenioController` inyecta la interface `DuenioService` (puerto de entrada), nunca la implementación:

- **Desacoplamiento:** el adaptador web depende de un contrato, no de una clase concreta. La
  implementación se puede reemplazar (otra lógica, un proxy, un mock en tests) sin tocar el controller.
- **Regla de dependencias:** `DuenioServiceImpl` vive en `application`; si el adaptador la importara,
  quedaría atado a un detalle de la capa interna en lugar de al puerto que el dominio expone.
- **Testabilidad:** en los tests del controller se mockea `DuenioService` directamente.

Por el mismo motivo el controller tampoco importa `domain.model`: recibe y devuelve DTOs, y la
traducción DTO ↔ dominio la hace `DuenioMapper`, que vive en el adaptador de entrada.

### Decisiones de modelado respecto del monolito

- **`Persona` se aplanó:** era un `@MappedSuperclass` (sin tabla propia) del que heredaban `Duenio` y
  `Veterinario`. Como los microservicios no comparten clases de dominio, sus campos (nombre, apellido,
  teléfono, email) se copian en cada entidad.
- **`Duenio` ya no tiene la lista de mascotas:** esa relación pasa a `ms-mascota`, que guarda `duenioId`.
- **Medicamento y Prescripción quedan fuera** del alcance de la versión en microservicios.
