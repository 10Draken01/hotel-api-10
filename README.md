# Sistema Distribuido de Gestión de Reservas Hoteleras

Plataforma backend distribuida basada en **Spring Boot, Spring Cloud, Netflix Eureka, Spring Security OAuth2 Resource Server y Oracle Database**, diseñada bajo principios de alta cohesión, bajo acoplamiento, seguridad perimetral e integridad de las reglas del negocio hotelero.

---

## 1. Visión general del proyecto

El **Sistema de Gestión de Reservas Hoteleras** automatiza y estandariza las operaciones centrales de administración hotelera:

- **Gestión de huéspedes:** registro, modificación, trazabilidad y control de unicidad de clientes.
- **Gestión de habitaciones:** control de inventario, capacidades, tarifas por noche y administración de estados.
- **Gestión de reservas:** asignación de habitaciones, cálculo de tarifas, check-in, check-out, cancelaciones y conservación del historial.
- **Auditoría y reportes:** consulta de información histórica, métricas y agregaciones para usuarios administrativos.

La solución utiliza una arquitectura de microservicios independientes, comunicados mediante REST y Spring Cloud OpenFeign. La autenticación y emisión de credenciales se delegan a un **Authorization Server centralizado**, mientras que los microservicios de dominio operan como OAuth2 Resource Servers.

### Tecnologías principales

| Tecnología | Propósito |
|---|---|
| Java | Lenguaje de programación |
| Spring Boot | Desarrollo de servicios backend |
| Spring Cloud Gateway | Enrutamiento y entrada única |
| Netflix Eureka | Registro y descubrimiento de servicios |
| Spring Cloud OpenFeign | Comunicación HTTP entre microservicios |
| Spring Security OAuth2 Resource Server | Validación de tokens JWT |
| Springdoc OpenAPI / Swagger UI | Documentación de API |
| Jakarta Bean Validation | Validación de datos |
| Oracle Database 23ai / Free | Persistencia relacional |
| Angular | Interfaz de usuario |

---

## 2. Arquitectura de la solución

La plataforma sigue una arquitectura de microservicios con comunicación síncrona, descubrimiento dinámico y un punto de entrada centralizado mediante Spring Cloud Gateway.

```text
                  ┌──────────────────────────────┐
                  │      Frontend Angular        │
                  └──────────────┬───────────────┘
                                 │
                       Bearer Token (JWT)
                                 │
                                 ▼
                  ┌──────────────────────────────┐
                  │   Spring Cloud Gateway       │
                  │          Puerto 8080         │
                  └──────────────┬───────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
                ▼                ▼                ▼
       ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
       │  MS Reservas   │ │  MS Huéspedes  │ │ MS Habitaciones│
       │     :8081      │ │     :8082      │ │     :8083      │
       └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
               │                 │                  │
               │                 └──────┐  ┌────────┘
               │                        │  │
               └──────── OpenFeign ─────┘  │
                          REST              │
                                            │
                                 ┌──────────▼───────────┐
                                 │   Oracle Database    │
                                 │       :1521          │
                                 └──────────────────────┘

                  ┌──────────────────────────────┐
                  │       Eureka Server          │
                  │          Puerto 8761         │
                  └──────────────────────────────┘

                  ┌──────────────────────────────┐
                  │    Authorization Server      │
                  │       Puerto configurable    │
                  └──────────────────────────────┘
```

### 2.1. Distribución de puertos

| Componente | Puerto | Responsabilidad |
|---|:---:|---|
| API Gateway | `8080` | Entrada única, seguridad perimetral y enrutamiento |
| Microservicio Reservas | `8081` | Orquestación de reservas y reportes |
| Microservicio Huéspedes | `8082` | Administración de huéspedes |
| Microservicio Habitaciones | `8083` | Administración del inventario hotelero |
| Eureka Server | `8761` | Registro y descubrimiento de servicios |
| Oracle Database | `1521` | Persistencia de datos |
| Authorization Server | Configurable | Emisión de tokens y gestión de identidades |

### 2.2. Componentes del ecosistema

| Componente | Responsabilidad técnica |
|---|---|
| **Spring Cloud Gateway** | Centraliza las solicitudes externas, valida los JWT conforme a su configuración de seguridad y enruta las peticiones. |
| **Netflix Eureka Server** | Registra las instancias de los microservicios y permite descubrir sus ubicaciones. |
| **Authorization Server** | Emite tokens OAuth2/OIDC y administra usuarios y roles. Es un componente externo al dominio del sistema. |
| **MS Reservas** | Coordina el ciclo de vida de las reservas, valida dependencias, calcula importes y genera reportes. |
| **MS Huéspedes** | Administra los datos maestros de los huéspedes, la unicidad de sus datos y el borrado lógico. |
| **MS Habitaciones** | Administra habitaciones, capacidades, precios, estados y disponibilidad. |
| **Oracle Database** | Almacena la información persistente de los servicios según la configuración de cada aplicación. |

---

## 3. Matriz de rutas del Gateway

Las solicitudes externas deben dirigirse al Gateway, utilizando el prefijo `/api/`.

| Prefijo de ruta | Servicio destino | Descripción |
|---|---|---|
| `/api/usuarios/**` | Authorization Server | Gestión administrativa de usuarios |
| `/api/huespedes/**` | Microservicio Huéspedes | Administración de huéspedes |
| `/api/habitaciones/**` | Microservicio Habitaciones | Catálogo, tarifas y mantenimiento |
| `/api/reservas/**` | Microservicio Reservas | Operaciones de reservas y reportes |

Las rutas anteriores representan el contrato externo. Los prefijos internos y las rutas finales deben coincidir con la configuración real del Gateway y de los controladores.

### Ejemplos de direcciones locales

```text
Gateway:
http://localhost:8080

Reservas:
http://localhost:8081

Huéspedes:
http://localhost:8082

Habitaciones:
http://localhost:8083

Eureka Dashboard:
http://localhost:8761
```

**Recomendación:** las aplicaciones cliente deben utilizar el Gateway como punto de entrada. Los puertos directos de los microservicios deben reservarse para tareas internas, pruebas y administración controlada.

---

## 4. Reglas de negocio y decisiones de diseño

### 4.1. Seguridad y control de acceso basado en roles (RBAC)

Cada microservicio actúa como OAuth2 Resource Server y protege sus operaciones mediante Spring Security y anotaciones como `@PreAuthorize`.

- **Rol `USER` — Recepcionista:** operaciones cotidianas de reservas, consultas y modificaciones autorizadas de huéspedes y habitaciones.
- **Rol `ADMIN` — Gerente:** permisos administrativos, borrados lógicos, modificación de tarifas, cambios manuales de estadoHabitacion y consulta de reportes gerenciales.

La autorización efectiva depende de las reglas configuradas en cada servicio. La validación de la firma y expiración del JWT no sustituye la comprobación de roles.

El sistema contempla una vigencia objetivo de **15 minutos para los tokens JWT**, según la configuración del Authorization Server.

### 4.2. Máquina de estados de habitaciones

Los estados de las habitaciones están restringidos a los siguientes valores:

- `DISPONIBLE`
- `OCUPADA`
- `LIMPIEZA`
- `MANTENIMIENTO`

Flujo general:

```text
                 ┌─────────────────┐
                 │   DISPONIBLE    │
                 └────────┬────────┘
                          │
               Creación de reserva
                          │
                          ▼
                 ┌─────────────────┐
                 │     OCUPADA     │
                 └────────┬────────┘
                          │
                       Check-out
                          │
                          ▼
                 ┌─────────────────┐
                 │     LIMPIEZA    │
                 └────────┬────────┘
                          │
                  Limpieza concluida
                          │
                          ▼
                 ┌─────────────────┐
                 │   DISPONIBLE    │
                 └─────────────────┘

       MANTENIMIENTO: estadoHabitacion administrativo
       controlado mediante operaciones autorizadas.
```

#### Transiciones automáticas

- **Crear reserva:** la habitación pasa de `DISPONIBLE` a `OCUPADA`.
- **Check-in:** la habitación permanece `OCUPADA`.
- **Check-out:** la reserva pasa a `FINALIZADA` y la habitación pasa a `LIMPIEZA`.
- **Cancelación:** la habitación regresa a `DISPONIBLE`, siempre que la cancelación sea válida.

#### Transiciones manuales

El endpoint administrativo previsto es:

`PATCH /{id}/estadoHabitacion/{idEstado}`

Reglas:

- Solo `ADMIN` puede ejecutar cambios manuales.
- Los cambios manuales permitidos deben respetar la máquina de estados definida por el negocio.
- No se permite cambiar manualmente una habitación `OCUPADA`.
- No se permite solicitar una transición hacia `OCUPADA` mediante la operación manual.
- Repetir el estadoHabitacion actual o solicitar una transición inválida devuelve `409 Conflict`.
- Un usuario `USER` que intente ejecutar la operación recibe `403 Forbidden`.

Los identificadores numéricos de estadoHabitacion deben corresponder al catálogo definido por el microservicio de Habitaciones.

### 4.3. Ciclo de vida y reglas de las reservas

**1. Inmutabilidad de las entidades asociadas**

Una vez creada una reserva, los campos `idHuesped` e `idHabitacion` no pueden cambiar bajo ningún estadoHabitacion. Un intento de modificarlos mediante `PUT` devuelve `409 Conflict`.

Si se necesita cambiar el huésped o la habitación, se cancela la reserva original cuando las reglas lo permitan y se crea otra.

**2. Capacidad y aforo**

El número de personas debe estar entre uno y la capacidad máxima de la habitación. Una solicitud que incumpla esta condición devuelve `409 Conflict`.

**3. Cálculo de tarifas**

- `precioNoche`: se obtiene del precio vigente de la habitación al crear la reserva.
- `noches`: diferencia en días entre la fecha de salida y la fecha de entrada.
- `precioTotal`: número de noches multiplicado por el precio por noche.

El precio por noche queda congelado para preservar el importe acordado al crear la reserva. Los cambios posteriores en la tarifa de la habitación no modifican reservas existentes.

Si las fechas se modifican mediante `PUT`, el precio total debe recalcularse en el backend.

**4. Modificación de fechas**

| Estado de la reserva | Regla de modificación |
|---|---|
| `CONFIRMADA` | Se pueden modificar ambas fechas, siempre que la entrada sea anterior a la salida. |
| `EN_CURSO` | Solo se permite modificar la fecha de salida, manteniéndola posterior a la entrada. |
| `FINALIZADA` | Solo consulta histórica; las modificaciones devuelven `409 Conflict`. |
| `CANCELADA` | Solo consulta histórica; las modificaciones devuelven `409 Conflict`. |

**5. Transiciones de estadoHabitacion de una reserva**

| Estado origen | Estado destino | Operación |
|---|---|---|
| `CONFIRMADA` | `EN_CURSO` | Check-in |
| `EN_CURSO` | `FINALIZADA` | Check-out |
| `CONFIRMADA` | `CANCELADA` | Cancelación |

Las transiciones inválidas, la repetición del estadoHabitacion actual y la cancelación de una reserva `EN_CURSO` devuelven `409 Conflict`.

El endpoint previsto es:

`PATCH /{idReserva}/estadoHabitacion/{idEstado}`

**6. Eliminación**

La eliminación de una reserva solo está permitida a usuarios `ADMIN` cuando el estadoHabitacion sea `FINALIZADA` o `CANCELADA`.

Las reservas `CONFIRMADA` o `EN_CURSO` no pueden eliminarse. La operación debe realizar un borrado lógico y conservar la trazabilidad.

### 4.4. Orquestación y compensación con OpenFeign

Al crear una reserva, el microservicio de Reservas coordina las operaciones siguientes:

1. Valida que el huésped exista y esté activo mediante una consulta al microservicio de Huéspedes.
2. Valida que la habitación exista, esté activa, se encuentre disponible y tenga capacidad suficiente.
3. Solicita el cambio de estadoHabitacion de la habitación a `OCUPADA`.
4. Calcula los importes y persiste la reserva.
5. Si la persistencia o un paso posterior falla después de ocupar la habitación, ejecuta una acción compensatoria para intentar restaurarla a `DISPONIBLE`.

La comunicación con los servicios de dominio se realiza mediante Spring Cloud OpenFeign y solicitudes HTTP.

La compensación debe registrar los fallos y manejar también el caso en el que la operación de reversión no pueda completarse. No constituye una transacción distribuida ACID: es una estrategia de consistencia eventual.

Los errores esperados incluyen:

- `404 Not Found`: huésped o habitación inexistente.
- `409 Conflict`: huésped eliminado, habitación no disponible o incumplimiento de reglas de negocio.
- `503 Service Unavailable`: dependencia remota no disponible.

### 4.5. Borrado lógico y unicidad

**Borrado lógico**

El sistema no debe eliminar físicamente los registros mediante las operaciones de negocio. En su lugar, actualiza `estadoRegistro` a `ELIMINADO`.

**Filtrado de consultas**

- Los listados públicos excluyen registros eliminados.
- Consultar por ID un registro eliminado devuelve `404 Not Found`.
- Los endpoints internos de auditoría permiten consultar información histórica independientemente del estadoHabitacion del registro.

**Unicidad entre registros activos**

La unicidad de campos como correo, teléfono, documento de identidad y número de habitación se comprueba exclusivamente contra registros activos, según las reglas de cada entidad.

El proceso de validación debe:

- Normalizar los datos antes de comparar.
- Excluir el identificador actual durante las actualizaciones.
- Permitir reutilizar valores liberados por registros eliminados, cuando la regla de negocio lo permita.

Si se decide no declarar restricciones `UNIQUE` en Oracle por la lógica de borrado lógico, deben considerarse las condiciones de concurrencia para evitar duplicados ante solicitudes simultáneas.

**Endpoints internos de auditoría**

- `GET /api/huespedes/id-huesped/{id}`
- `GET /api/habitaciones/id-habitacion/{id}`

Estos endpoints permiten reconstruir el histórico de una reserva incluso cuando el huésped o la habitación se encuentren eliminados lógicamente.

---

## 5. Esquema de base de datos: Oracle Database

El siguiente DDL representa el esquema inicial de las tres entidades principales para Oracle Database 23ai / Free.

Cadena de conexión:

`jdbc:oracle:thin:@//localhost:1521/FREE`

### 5.1. Creación del usuario

Ejecutar con una cuenta administrativa autorizada:

```sql
CREATE USER hotel_user IDENTIFIED BY "CAMBIAR_POR_UN_SECRETO_SEGURO";

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO hotel_user;

GRANT UNLIMITED TABLESPACE TO hotel_user;
```

Si el entorno requiere privilegios adicionales, deben concederse de acuerdo con las políticas de seguridad de Oracle. No se recomienda habilitar `_ORACLE_SCRIPT` salvo que sea necesario para un entorno específico.

Conectarse posteriormente con el usuario `hotel_user` para crear las tablas.

### 5.2. Tabla HUESPED

```sql
CREATE TABLE huesped (
    id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre           VARCHAR2(50)  NOT NULL,
    apellido         VARCHAR2(50)  NOT NULL,
    email            VARCHAR2(100) NOT NULL,
    telefono         VARCHAR2(10)  NOT NULL,
    tipo_documento   VARCHAR2(20)  NOT NULL,
    numero_documento VARCHAR2(20) NOT NULL,
    nacionalidad     VARCHAR2(50)  NOT NULL,
    estado_registro  VARCHAR2(20) DEFAULT 'ACTIVO' NOT NULL
);
```

### 5.3. Tabla HABITACION

```sql
CREATE TABLE habitacion (
    id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero           NUMBER       NOT NULL,
    tipo             VARCHAR2(20) NOT NULL,
    precio           NUMBER(10,2) NOT NULL,
    capacidad        NUMBER       NOT NULL,
    estadoHabitacion           NUMBER(1) DEFAULT 1 NOT NULL,
    estado_registro  VARCHAR2(20) DEFAULT 'ACTIVO' NOT NULL
);
```

Estados de habitación:

- `1`: DISPONIBLE
- `2`: OCUPADA
- `3`: LIMPIEZA
- `4`: MANTENIMIENTO

### 5.4. Tabla RESERVA

```sql
CREATE TABLE reserva (
    id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_huesped       NUMBER        NOT NULL,
    id_habitacion    NUMBER        NOT NULL,
    fecha_entrada    DATE          NOT NULL,
    fecha_salida     DATE          NOT NULL,
    numero_personas  NUMBER        NOT NULL,
    precio_noche     NUMBER(10,2)  NOT NULL,
    precio_total     NUMBER(12,2)  NOT NULL,
    estadoHabitacion           NUMBER(1) DEFAULT 1 NOT NULL,
    estado_registro  VARCHAR2(20) DEFAULT 'ACTIVO' NOT NULL
);
```

Estados de reserva:

- `1`: CONFIRMADA
- `2`: EN_CURSO
- `3`: FINALIZADA
- `4`: CANCELADA

**Consideraciones de persistencia**

- Las entidades JPA deben utilizar nombres de tabla y columna que coincidan con este DDL.
- Los campos monetarios deben representarse con `BigDecimal`.
- Los identificadores se pueden representar mediante `Long`.
- Los campos `DATE` requieren una decisión explícita sobre si se utilizará `LocalDate` o `LocalDateTime`, ya que Oracle también puede almacenar la hora.
- Las restricciones de fechas, importes, aforo y estados deben protegerse tanto en la lógica de negocio como en la base de datos cuando corresponda.
- Los identificadores de huésped y habitación no necesitan relaciones JPA entre microservicios independientes.

---

## 6. Estandarización de respuestas y errores HTTP

El sistema utiliza un manejador global de excepciones con `@RestControllerAdvice` para estandarizar las respuestas de error.

| Código HTTP | Condición | Ejemplo |
|---|---|---|
| `400 Bad Request` | Solicitud inválida o incumplimiento de validaciones de entrada | Fecha de entrada posterior o igual a la salida |
| `401 Unauthorized` | Autenticación ausente o token inválido/expirado | JWT no válido |
| `403 Forbidden` | Usuario autenticado sin permisos suficientes | `USER` intenta consultar reportes administrativos |
| `404 Not Found` | Recurso inexistente o eliminado lógicamente en consultas públicas | Huésped dado de baja |
| `409 Conflict` | Incumplimiento de reglas de negocio | Habitación ocupada o transición inválida |
| `500 Internal Server Error` | Fallo inesperado del servidor | Excepción no contemplada |
| `503 Service Unavailable` | Dependencia remota no disponible | Servicio de Huéspedes inaccesible durante una reserva |

Las respuestas deben mantener un formato homogéneo, preferiblemente `application/problem+json`, e incluir información útil sin exponer trazas internas, credenciales ni detalles sensibles de infraestructura.

---

## 7. Plan de trabajo Scrum

El desarrollo del backend se organiza en un sprint de tres días.

### Día 1: Cimientos y catálogos independientes

- **Draken:** creación del usuario Oracle, DDL y desarrollo del microservicio de Huéspedes, incluyendo CRUD, unicidad y borrado lógico.
- **Lalo:** desarrollo del microservicio de Habitaciones, incluyendo CRUD, unicidad y cambios manuales de estadoHabitacion.

### Día 2: Orquestador y reglas transaccionales

- **Draken:** microservicio de Reservas, configuración OpenFeign, validaciones cruzadas, creación de reservas y compensación ante fallos.
- **Lalo:** máquina de estados de reservas, actualización de fechas, recálculo de precios y eliminación lógica.

### Día 3: Seguridad, reportes y pruebas

- **Draken:** endpoints de reportes, historial y reservas por estadoHabitacion; configuración Resource Server JWT en Huéspedes y Reservas.
- **Lalo:** configuración Resource Server JWT en Habitaciones y ejecución de los 25 casos de aceptación.

---

## 8. Casos de prueba críticos de aceptación

Los siguientes escenarios deben validarse mediante solicitudes HTTP, preferiblemente a través del Gateway.

| # | Módulo | Caso de prueba | Resultado esperado |
|---:|---|---|---|
| 01 | Reservas | Crear reserva con habitación disponible | `201 Created`; habitación pasa a OCUPADA |
| 02 | Reservas | Crear reserva con habitación ocupada | `409 Conflict` |
| 03 | Reservas | Crear reserva con habitación en LIMPIEZA o MANTENIMIENTO | `409 Conflict` |
| 04 | Reservas | Check-in de reserva confirmada | `200 OK`; habitación sigue OCUPADA |
| 05 | Reservas | Check-out de reserva en curso | `200 OK`; habitación pasa a LIMPIEZA |
| 06 | Reservas | Check-out de reserva confirmada | `409 Conflict` |
| 07 | Reservas | Cancelar reserva confirmada | `200 OK`; habitación pasa a DISPONIBLE |
| 08 | Reservas | Cancelar reserva en curso | `409 Conflict` |
| 09 | Reservas | Modificar fechas antes del check-in | `200 OK`; precio total recalculado |
| 10 | Reservas | Modificar fecha de entrada después del check-in | `409 Conflict` |
| 11 | Reservas | Modificar fecha de salida durante la estancia | `200 OK`; precio total recalculado |
| 12 | Reservas | Cambiar huésped o habitación de una reserva | `409 Conflict` |
| 13 | Reservas | Verificar cálculo del importe por número de noches | Importe calculado en backend |
| 14 | Reservas | Eliminar reserva confirmada o en curso | `409 Conflict` |
| 15 | Habitaciones | Eliminar habitación disponible | `200 OK`; borrado lógico |
| 16 | Habitaciones | Eliminar habitación ocupada | `409 Conflict` |
| 17 | Habitaciones | ADMIN cambia habitación de LIMPIEZA a DISPONIBLE | `200 OK` |
| 18 | Habitaciones | ADMIN intenta cambiar manualmente una habitación OCUPADA | `409 Conflict` |
| 19 | Habitaciones | USER intenta realizar un cambio manual de estadoHabitacion | `403 Forbidden` |
| 20 | Huéspedes | Registrar o actualizar con correo duplicado activo | `409 Conflict` |
| 21 | Huéspedes | Registrar con correo de un huésped eliminado | `201 Created` |
| 22 | Huéspedes | Registrar correo con mayúsculas | `201 Created`; correo normalizado |
| 23 | Huéspedes | Eliminar huésped con reservas confirmadas o en curso | `409 Conflict` |
| 24 | Reportes | Consultar reportes con rol USER | `403 Forbidden` |
| 25 | Reportes | Consultar reportes con rol ADMIN | `200 OK`; información histórica según los filtros |

Los códigos indicados son los resultados esperados del contrato de la API. Deben verificarse contra las implementaciones reales de los controladores, servicios y manejadores de excepciones.

---

## 9. Configuración de variables de entorno

Cada microservicio de dominio debe disponer de su propio archivo `.env` local, con los parámetros correspondientes a su puerto, conexión a Oracle y comunicación con el Gateway y el Authorization Server.

### 9.1. Plantilla general

```dotenv
PORT=8081

DB_URL=jdbc:oracle:thin:@//localhost:1521/FREE
DB_USER=hotel_user
DB_PASS=${DB_PASS}

BASE_URL=http://localhost:8081
GATEWAY_URL=http://localhost:8080/api/reservas

ISSUER_URI=http://localhost:9000
```

Esta plantilla corresponde al microservicio de Reservas. Los valores de puerto y ruta deben ajustarse para cada servicio.

### 9.2. Valores por microservicio

| Microservicio | `PORT` | `BASE_URL` | `GATEWAY_URL` |
|---|---:|---|---|
| Reservas | `8081` | `http://localhost:8081` | `http://localhost:8080/api/reservas` |
| Huéspedes | `8082` | `http://localhost:8082` | `http://localhost:8080/api/huespedes` |
| Habitaciones | `8083` | `http://localhost:8083` | `http://localhost:8080/api/habitaciones` |

### 9.3. Descripción de las variables

| Variable | Descripción |
|---|---|
| `PORT` | Puerto en el que escucha el microservicio. |
| `DB_URL` | Cadena de conexión JDBC de Oracle. |
| `DB_USER` | Usuario de la base de datos. |
| `DB_PASS` | Contraseña del usuario de la base de datos. |
| `BASE_URL` | Dirección local del servicio. |
| `GATEWAY_URL` | Dirección del Gateway con el prefijo de ruta del servicio. |
| `ISSUER_URI` | Dirección del emisor de los JWT. |

**Importante:**

- Spring Boot no carga archivos `.env` automáticamente en todas las configuraciones. Es necesario utilizar un mecanismo compatible o exportar las variables al entorno.
- La variable `DB_PASS` debe estar definida en el entorno de ejecución; el valor `${DB_PASS}` es una referencia, no una contraseña literal.
- Los archivos `.env` locales deben incluirse en `.gitignore`.
- No deben versionarse credenciales reales, tokens ni secretos.
- La dirección de `ISSUER_URI` debe coincidir con el emisor real de los tokens.
- Las direcciones de los servicios deben ajustarse si la aplicación se ejecuta dentro de Docker u otra infraestructura de despliegue.

---

## 10. Despliegue y ejecución local

### 10.1. Requisitos previos

Antes de iniciar el sistema, verifica que estén instalados y configurados:

- Java en la versión compatible con el proyecto.
- Maven o el sistema de construcción utilizado.
- Oracle Database 23ai / Free, local o en contenedor.
- Eureka Server.
- Spring Cloud Gateway.
- Authorization Server.
- Los tres microservicios de dominio.
- Angular y Node.js, si se ejecuta el frontend.

### 10.2. Orden recomendado de inicialización

1. **Oracle Database:** iniciar la base de datos en el puerto `1521`, crear el usuario y ejecutar el DDL.
2. **Authorization Server:** iniciar el proveedor de identidad y verificar la emisión de tokens.
3. **Eureka Server:** iniciar el servicio en `8761` y comprobar su panel de administración.
4. **Spring Cloud Gateway:** iniciar el punto de entrada en `8080`.
5. **Microservicio Reservas:** iniciar en `8081`.
6. **Microservicio Huéspedes:** iniciar en `8082`.
7. **Microservicio Habitaciones:** iniciar en `8083`.
8. **Frontend Angular:** iniciar la aplicación cliente y configurar sus solicitudes para utilizar el Gateway.

Los microservicios pueden iniciarse en otro orden si su configuración permite recuperarse de dependencias temporalmente no disponibles. En todos los casos, es necesario comprobar su registro en Eureka y su conectividad con Oracle.

### 10.3. Verificaciones posteriores al arranque

- Eureka está disponible en `http://localhost:8761`.
- El Gateway escucha en `http://localhost:8080`.
- Reservas escucha en `http://localhost:8081`.
- Huéspedes escucha en `http://localhost:8082`.
- Habitaciones escucha en `http://localhost:8083`.
- Las conexiones JDBC funcionan correctamente.
- Los microservicios se registran en Eureka.
- Las llamadas OpenFeign llegan a los servicios correspondientes.
- Los endpoints protegidos rechazan solicitudes sin autenticación válida.
- Los roles se aplican correctamente.
- Swagger UI está disponible según la configuración de cada aplicación.

---

## 11. Documentación de API

Los microservicios pueden exponer documentación OpenAPI mediante Springdoc y Swagger UI.

La documentación debe describir:

- Los endpoints disponibles y sus parámetros.
- Los DTO de solicitud y respuesta.
- Los códigos HTTP y formatos de error.
- Las restricciones de validación.
- Los esquemas de autenticación JWT.
- Los roles autorizados para cada operación.

Las rutas exactas de Swagger UI dependen de la configuración de cada servicio y del Gateway. Si se desea centralizar la documentación, el Gateway puede configurarse para enrutar las solicitudes de Swagger a cada microservicio.

---

## 12. Consideraciones de calidad y seguridad

Para mantener la estabilidad y la integridad del sistema, se recomienda:

- Aplicar validaciones en los DTO y en la capa de servicio.
- Centralizar el manejo de excepciones con respuestas HTTP homogéneas.
- Evitar exponer entidades JPA directamente en la API cuando sea preferible utilizar DTO.
- Calcular los precios en el backend.
- Registrar los errores de comunicación entre servicios.
- Implementar tiempos de espera adecuados para OpenFeign.
- Proteger los endpoints según los roles definidos.
- Evitar que el frontend se comunique directamente con los microservicios.
- Mantener las credenciales fuera del repositorio.
- Probar las compensaciones ante fallos y las solicitudes concurrentes que afectan la disponibilidad de habitaciones.
- Asegurar que las transiciones de estadoHabitacion y las operaciones de borrado lógico sean consistentes.
- Mantener los contratos de API alineados con el esquema de Oracle y la documentación OpenAPI.

---

## 13. Estado del proyecto

El proyecto está estructurado en tres microservicios de dominio independientes, con un Gateway como punto de entrada, Eureka para descubrimiento de servicios, un Authorization Server externo para autenticación y Oracle Database como sistema de persistencia.

La implementación se orienta a mantener la independencia de los servicios, centralizar el acceso, proteger las operaciones sensibles y preservar la integridad de las reservas durante todo el ciclo de hospedaje.
