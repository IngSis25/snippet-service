# Snippet Service

Microservicio encargado de gestionar snippets de código, tests y configuraciones de usuarios.

Forma parte del sistema Snippet Searcher, junto con:

- **runner-service**: procesa los snippets usando la librería printscript (validación, ejecución, formateo, lint).
- **authorization-service**: maneja autenticación y permisos de usuarios.
- **snippet-service** (este): gestiona snippets, tests y configuraciones.

## Propósito

El snippet-service es el núcleo del sistema: almacena y gestiona todos los snippets de código, sus tests asociados y las configuraciones de los usuarios.

Se comunica con el runner-service mediante Redis Streams para delegar tareas de procesamiento (validación, ejecución, formateo, lint) y con el authorization-service para gestionar permisos y compartir snippets entre usuarios.

## Funcionalidades principales

- **Gestión de snippets**: crear, actualizar, eliminar y consultar snippets de código.
- **Gestión de tests**: crear, eliminar y ejecutar tests asociados a snippets.
- **Gestión de lenguajes**: soporte para múltiples lenguajes de programación (actualmente PrintScript).
- **Compartir snippets**: compartir snippets con otros usuarios del sistema.
- **Filtrado y búsqueda**: filtrar snippets por nombre, lenguaje, estado de validación y rol del usuario.
- **Descarga de snippets**: descargar snippets en su versión original o formateada.
- **Integración con asset-service**: almacenamiento de contenido de snippets y resultados de lint.
- **Integración con authorization-service**: gestión de permisos y roles de usuarios.
- **Comunicación asíncrona**: publicación de eventos a runner-service mediante Redis Streams para procesamiento asíncrono.

## Tecnologías

- **Spring Boot 3.5.6**: framework principal
- **Kotlin**: lenguaje de programación
- **PostgreSQL**: base de datos principal
- **Redis Streams**: comunicación asíncrona con runner-service
- **Spring Security OAuth2**: autenticación mediante Auth0
- **JPA/Hibernate**: ORM para acceso a datos

## Configuración

### Variables de entorno

El servicio requiere las siguientes variables de entorno:

#### Base de datos (PostgreSQL)
- `DB_USER`: Usuario de la base de datos
- `DB_PASSWORD`: Contraseña de la base de datos
- `DB_NAME`: Nombre de la base de datos
- `DB_HOST`: Host de la base de datos
- `DB_PORT`: Puerto de la base de datos (default: 5433)

#### Redis
- `REDIS_HOST`: Host de Redis
- `REDIS_PORT`: Puerto de Redis (default: 6379)

#### Auth0
- `AUTH_SERVER_URI`: URI del servidor de autenticación (Auth0)
- `AUTH_CLIENT_ID`: Client ID de Auth0
- `AUTH_CLIENT_SECRET`: Client Secret de Auth0
- `AUTH_AUDIENCE`: Audience de Auth0 para validación de tokens

### Base de datos

El servicio utiliza PostgreSQL como base de datos. La configuración se encuentra en `application.yaml`.

### Redis

Redis se utiliza para la comunicación asíncrona con el runner-service mediante Redis Streams.

## Endpoints principales

- `GET /api/languages/all`: Obtener todos los lenguajes disponibles
- `POST /api/languages/`: Crear un nuevo lenguaje
- `GET /api/snippets/{id}`: Obtener un snippet por ID
- `POST /api/snippets`: Crear un nuevo snippet
- `PUT /api/snippets/{id}`: Actualizar un snippet
- `POST /api/snippets/delete/{id}`: Eliminar un snippet
- `GET /api/snippets/user`: Obtener snippets del usuario con filtros
- `POST /api/snippets/share/{id}`: Compartir un snippet
- `POST /api/snippets/format/{id}`: Solicitar formateo de un snippet
- `PUT /api/snippets/{id}/status`: Actualizar estado de validación de un snippet
- `GET /api/snippets/{id}/download`: Descargar un snippet
- `GET /api/snippets/{id}/download/formatted`: Descargar un snippet formateado
- `GET /api/tests/snippet/{snippetId}`: Obtener tests de un snippet
- `POST /api/tests/snippet/{snippetId}`: Crear un test para un snippet
- `DELETE /api/tests/{id}`: Eliminar un test
- `POST /api/tests/{id}/run`: Ejecutar un test
- `POST /api/tests/snippet/{snippetId}/run-all`: Ejecutar todos los tests de un snippet

## Ejecución

### Requisitos

- Java 17+
- PostgreSQL
- Redis

### Ejecución local

1. Configurar las variables de entorno en un archivo `.env` o como variables del sistema
2. Asegurarse de que PostgreSQL y Redis estén ejecutándose
3. Ejecutar `./gradlew bootRun`

### Tests

Ejecutar los tests con:

```bash
./gradlew test
```

El proyecto requiere una cobertura de código mínima del 80% (configurado con JaCoCo).

## Arquitectura

El servicio sigue una arquitectura de microservicios con las siguientes capas:

- **API Layer**: Controladores REST que exponen los endpoints
- **Service Layer**: Lógica de negocio
- **Repository Layer**: Acceso a datos mediante JPA
- **Integration Layer**: Clientes para comunicación con otros servicios (authorization-service, asset-service)
- **Messaging Layer**: Productor de mensajes para Redis Streams
