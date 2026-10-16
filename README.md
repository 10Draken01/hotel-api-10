# Sistema Distribuido de Gestión de Reservas Hoteleras

Plataforma backend distribuida basada en microservicios con **Spring Boot**, **Spring Cloud**, **Netflix Eureka**, **Spring Security OAuth2 Resource Server** y base de datos relacional **Oracle Database**, diseñada bajo principios de alta cohesión, bajo acoplamiento, seguridad perimetral y estricta integridad en las reglas del negocio hotelero.

---

## 1. Visión General del Proyecto

El **Sistema de Gestión de Reservas Hoteleras** automatiza y estandariza las operaciones centrales de administración hotelera:
- **Gestión integral de huéspedes:** Registro, modificación, trazabilidad y control de unicidad de clientes.
- **Gestión operativa de habitaciones:** Control de inventario, capacidades, tarifas por noche y una máquina de estados controlada (`DISPONIBLE`, `OCUPADA`, `LIMPIEZA`, `MANTENIMIENTO`).
- **Orquestación de reservas:** Flujo transaccional de asignación, cálculo automatizado de tarifas por estancia, transiciones de estado (Check-in, Check-out, Cancelación) y auditoría con históricos inmutables.
- **Auditoría y reportes gerenciales:** Métricas y agregaciones consolidadas para roles administrativos.

La solución delega la autenticación perimetral y expedición de credenciales a un **Authorization Server centralizado preexistente**, operando los microservicios de dominio estrictamente como **Resource Servers**.

---

## 2. Arquitectura de la Solución

El sistema sigue un patrón de microservicios distribuidos comunicados de forma síncrona mediante REST y OpenFeign, con descubrimiento dinámico y punto de entrada único:

```text
               ┌───────────────────────────────────┐
               │     Frontend Angular Client       │
               └─────────────────┬─────────────────┘
                                 │
                                 ▼ (Bearer JWT / 15 min TTL)
               ┌───────────────────────────────────┐
               │    Spring Cloud Reactive Gateway  │  (Valida firma y expiración 401)
               │          (Puerto :8080)           │  (Enrutamiento dinámico)
               └─────────────────┬─────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │ (Registro y Consulta) │                       │
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌────────────────────────┐
│  Eureka Server   │    │   MS Huéspedes   │    │    MS Habitaciones     │
│   (:8761)        │    │    (:8081)       │    │        (:8082)         │
└──────────────────┘    └────────▲─────────┘    └───────────▲────────────┘
                                 │                          │
                                 │ (OpenFeign)              │ (OpenFeign)
                                 │                          │
                                 └────────────┬─────────────┘
                                              │
                                   ┌──────────┴─────────────┐
                                   │      MS Reservas       │
                                   │      (Orquestador)     │
                                   │         (:8083)        │
                                   └──────────┬─────────────┘
                                              │
                                              ▼
                        ┌───────────────────────────────────────────┐
                        │    Oracle Database 23ai / Free Container  │
                        │    (oracle:thin:@//localhost:1521/FREE)   │
                        │      Tablas: HUESPED, HABITACION, RESERVA │
                        └───────────────────────────────────────────┘
```

### Componentes del Ecosistema

| Componente | Rol y Responsabilidad Técnica |
| :--- | :--- |
| **Netflix Eureka Server** | Registro y descubrimiento dinámico de instancias backend. |
| **Spring Cloud Gateway** | Punto de entrada único perimetral. Inspecciona y valida la firma y vigencia del JWT (`401 Unauthorized` si es inválido o expiró) y enruta hacia los servicios según el prefijo de URI. |
| **Authorization Server** *(Proporcionado)* | Proveedor de identidad OAuth2/OIDC. Emite tokens JWT con tiempo de expiración estricto de 15 minutos y gestiona el CRUD de usuarios y roles (`ADMIN`, `USER`). |
| **MS Huéspedes** | Gestión de datos maestros de clientes, validaciones de formato, borrado lógico y endpoints de auditoría interna. |
| **MS Habitaciones** | Gestión del inventario físico de habitaciones, tarifas base y transiciones manuales de estado mediante `PATCH`. |
| **MS Reservas** | Orquestador principal del ciclo de vida de hospedaje. Coordina con Huéspedes y Habitaciones mediante Spring Cloud OpenFeign, ejecuta mecanismos de compensación, calcula costos y genera reportes ejecutivos. |

---

## 3. Matriz de Rutas del Gateway

Toda llamada externa debe dirigirse a la URL base del Gateway (`/api/*`):

| Prefijo de Ruta | Microservicio Destino | Descripción |
| :--- | :--- | :--- |
| `/api/usuarios/**` | Authorization Server | Gestión administrativa de usuarios del sistema |
| `/api/huespedes/**` | Microservicio Huéspedes | Administración de perfiles de clientes |
| `/api/habitaciones/**` | Microservicio Habitaciones | Catálogo físico y mantenimiento de habitaciones |
| `/api/reservas/**` | Microservicio Reservas | Motor transaccional de reservas y reportes |

---

## 4. Reglas de Negocio y Decisiones de Diseño

### 4.1. Seguridad y Control de Acceso Basado en Roles (RBAC)
Cada microservicio actúa como un **OAuth2 Resource Server** que decodifica el JWT y asegura los endpoints mediante `@PreAuthorize`:
- **Rol `USER` (Recepcionista):** Operaciones cotidianas de reserva (creación, check-in, check-out, cancelación), consulta y modificación de habitaciones y huéspedes.
- **Rol `ADMIN` (Gerente):** Privilegios totales: borrado lógico de entidades, modificación de tarifas y propiedades físicas de habitaciones, transiciones manuales de estado de habitaciones y consulta de reportes gerenciales.

### 4.2. Máquina de Estados de Habitaciones
El estado de una habitación está estrictamente acotado:

```text
       ┌──────────────┐   (Manual: Limpieza concluida)
       │              │◄────────────────────────────────┐
       ▼              │                                 │
[ DISPONIBLE ] ───┬───┴───────────────┐                 │
       │          │                   │                 │
       │ (Auto:   │ (Manual PATCH)    │ (Manual PATCH)  │
       │ Reserva) │                   │                 │
       ▼          ▼                   ▼                 │
 [ OCUPADA ]   [ MANTENIMIENTO ]   [ LIMPIEZA ] ────────┘
       │                               ▲
       └───────────────────────────────┘
           (Auto: Check-out reserva)
```

- **Transiciones automáticas (por ciclo de reserva):**
    - **Crear reserva:** La habitación pasa inmediatamente de `DISPONIBLE` a `OCUPADA`.
    - **Check-in:** La habitación permanece `OCUPADA`.
    - **Check-out:** La reserva pasa a `FINALIZADA` y la habitación pasa automáticamente a `LIMPIEZA`.
    - **Cancelación:** La habitación `OCUPADA` regresa a `DISPONIBLE`.
- **Transiciones manuales (`PATCH /{id}/estado/{idEstado}` solo ADMIN):**
    - Permitidas únicamente entre `DISPONIBLE`, `LIMPIEZA` y `MANTENIMIENTO`.
    - Intentar cambiar manualmente hacia `OCUPADA`, transicionar una habitación que ya esté `OCUPADA`, o repetir el estado actual responde `409 Conflict`.
    - Un usuario con rol `USER` que intente ejecutar un cambio manual recibe `403 Forbidden`.

### 4.3. Ciclo de Vida y Reglas de la Reserva
1. **Inmutabilidad de entidades base:** Una vez creada la reserva, el `idHuesped` y el `idHabitacion` **no pueden cambiarse bajo ningún estado** (un `PUT` con IDs distintos retorna `409 Conflict`). Si se requiere cambiar de habitación o cliente, la reserva debe cancelarse y emitirse una nueva.
2. **Capacidad y Aforo:** El `numeroPersonas` debe encontrarse entre 1 y la capacidad máxima declarada en la habitación; de lo contrario responde `409 Conflict`.
3. **Cálculo de Tarifas:**
    - `precioNoche`: Se congela al momento de la creación de la reserva tomando el precio vigente de la habitación (los cambios de precio posteriores de la habitación no afectan reservas ya creadas).
    - `noches`: Calculado como $\text{fechaSalida} - \text{fechaEntrada}$ en días.
    - `precioTotal`: Calculado y almacenado como $\text{noches} \times \text{precioNoche}$. Se recalcula automáticamente si se modifican las fechas mediante `PUT`.
4. **Modificación de Fechas (`PUT`):**
    - En estado `CONFIRMADA`: Se permite modificar tanto `fechaEntrada` como `fechaSalida` (siempre que $\text{fechaEntrada} < \text{fechaSalida}$).
    - En estado `EN_CURSO`: Solo se permite modificar la `fechaSalida` (acortar o extender la estancia, exigiendo $\text{fechaSalida} > \text{fechaEntrada}$). Modificar la fecha de entrada responde `409 Conflict`.
    - En estados `FINALIZADA` o `CANCELADA`: El registro es de consulta histórica; cualquier intento de modificación responde `409 Conflict`.
5. **Transiciones con `PATCH /{idReserva}/estado/{idEstado}`:**
    - `1 -> 2` (CONFIRMADA $\rightarrow$ EN_CURSO): Check-in.
    - `2 -> 3` (EN_CURSO $\rightarrow$ FINALIZADA): Check-out (dispara paso de habitación a `LIMPIEZA`).
    - `1 -> 4` (CONFIRMADA $\rightarrow$ CANCELADA): Cancelación (dispara liberación de habitación a `DISPONIBLE`).
    - Intentar cancelar desde `EN_CURSO` responde `409 Conflict`.
    - Cualquier otra transición o repetición de estado responde `409 Conflict`.
6. **Eliminación (`DELETE`):** Solo permitida a usuarios `ADMIN` sobre reservas en estado `FINALIZADA` o `CANCELADA`. Eliminar una reserva `CONFIRMADA` o `EN_CURSO` responde `409 Conflict`.

### 4.4. Orquestación y Compensación con OpenFeign
Al crear una reserva, el microservicio de Reservas coordina las siguientes llamadas:
1. Valida la existencia y estado activo del Huésped vía Feign (`GET /api/huespedes/{id}`). Si no existe responde `404`; si está eliminado responde `409`.
2. Valida la Habitación y la bloquea cambiando su estado a `OCUPADA` (`PATCH /api/habitaciones/{id}/estado/2`). Si la habitación no está `DISPONIBLE` o está eliminada responde `409`.
3. Persiste la Reserva en base de datos.
4. **Mecanismo de compensación:** Si la persistencia o cualquier paso posterior falla, el servicio emite una llamada compensatoria revirtiendo el estado de la habitación a `DISPONIBLE` para evitar registros huérfanos o inconsistencias en inventario.

### 4.5. Borrado Lógico y Unicidad en la Capa de Servicio
- **Borrado Lógico:** Queda prohibido el `DELETE` físico en la base de datos relacional. La operación de eliminación actualiza el atributo `estadoRegistro = 'ELIMINADO'`.
- **Filtro de Listados:** Los endpoints de consulta pública (`GET /`) excluyen registros `ELIMINADO`. Consultar por ID un elemento eliminado responde `404 Not Found`.
- **Unicidad entre ACTIVOS:** Los campos únicos (Email, Teléfono y Documento en Huéspedes; Número en Habitaciones) son evaluados exclusivamente contra registros en estado `ACTIVO`. Un registro `ELIMINADO` libera el valor para reasignación futura.
- **Implementación:** Por este motivo, **no se declaran restricciones `UNIQUE` en la base de datos**. La unicidad se garantiza programáticamente en la capa de `@Service` tras sanitizar los valores (`trim()`, minúsculas en correos y dígitos puros en números telefónicos), excluyendo el propio ID durante actualizaciones (`PUT`/`PATCH`).
- **Endpoints Internos para Auditoría:** Para que los reportes de reservas puedan reconstruir el histórico exacto sin perder trazabilidad, cada microservicio expone:
    - `GET /api/huespedes/id-huesped/{id}`
    - `GET /api/habitaciones/id-habitacion/{id}`
      Estos endpoints devuelven la entidad sin importar si su `estadoRegistro` es `ACTIVO` o `ELIMINADO`.

---

## 5. Esquema de Base de Datos (Oracle Database)

Script DDL optimizado para despliegue en contenedor **Oracle Database 23ai / Free** (`oracle:thin:@//localhost:1521/FREE`):

```sql
-- 1. Habilitar script y crear esquema de base de datos
ALTER SESSION SET "_ORACLE_SCRIPT"=true;
CREATE USER hotel_user IDENTIFIED BY HotelPass2026#;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO hotel_user;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO hotel_user;

-- Conectarse como hotel_user para crear las tablas
-- ALTER SESSION SET CURRENT_SCHEMA = hotel_user;

-- 2. Tabla HUESPED
CREATE TABLE huesped (
                         id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         nombre           VARCHAR2(50)  NOT NULL,
                         apellido         VARCHAR2(50)  NOT NULL,
                         email            VARCHAR2(100) NOT NULL,
                         telefono         VARCHAR2(10)  NOT NULL,
                         tipo_documento   VARCHAR2(20)  NOT NULL, -- INE, LICENCIA, PASAPORTE
                         numero_documento VARCHAR2(20)  NOT NULL,
                         nacionalidad     VARCHAR2(50)  NOT NULL,
                         estado_registro  VARCHAR2(20)  DEFAULT 'ACTIVO' NOT NULL
);

-- 3. Tabla HABITACION
CREATE TABLE habitacion (
                            id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            numero           NUMBER        NOT NULL,
                            tipo             VARCHAR2(20)  NOT NULL, -- SENCILLA, DOBLE, SUITE
                            precio           NUMBER(10,2)  NOT NULL,
                            capacidad        NUMBER        NOT NULL,
                            estado           NUMBER(1)     DEFAULT 1 NOT NULL, -- 1: DISPONIBLE, 2: OCUPADA, 3: LIMPIEZA, 4: MANTENIMIENTO
                            estado_registro  VARCHAR2(20)  DEFAULT 'ACTIVO' NOT NULL
);

-- 4. Tabla RESERVA
CREATE TABLE reserva (
                         id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         id_huesped       NUMBER        NOT NULL,
                         id_habitacion    NUMBER        NOT NULL,
                         fecha_entrada    DATE          NOT NULL,
                         fecha_salida     DATE          NOT NULL,
                         numero_personas  NUMBER        NOT NULL,
                         precio_noche     NUMBER(10,2)  NOT NULL,
                         precio_total     NUMBER(12,2)  NOT NULL,
                         estado           NUMBER(1)     DEFAULT 1 NOT NULL, -- 1: CONFIRMADA, 2: EN_CURSO, 3: FINALIZADA, 4: CANCELADA
                         estado_registro  VARCHAR2(20)  DEFAULT 'ACTIVO' NOT NULL
);
```

---

## 6. Estandarización de Respuestas y Errores HTTP

El sistema cuenta con un controlador global de excepciones (`@RestControllerAdvice`) que homologa la semántica de errores en toda la plataforma:

| Código | Condición de Disparo | Ejemplo |
| :---: | :--- | :--- |
| **`400`** | Formato de payload inválido, violación de validaciones de campo (`@Valid`) o ID de estado fuera de catálogo. | Teléfono con longitud distinta de 10 dígitos; fechaEntrada >= fechaSalida. |
| **`401`** | Petición sin encabezado `Authorization`, token con firma corrupta o token expirado (> 15 minutos). | Interceptor del Gateway rechaza la petición. |
| **`403`** | Token válido emitido para rol `USER` intentando consumir recursos o métodos exclusivos de `ADMIN`. | Recepcionista intentando consultar `/api/reservas/reportes/*`. |
| **`404`** | Identificador inexistente o recurso con `estadoRegistro = 'ELIMINADO'` en consultas públicas. | Búsqueda por ID de un huésped dado de baja. |
| **`409`** | Incumplimiento de invariantes de negocio o máquina de estados. | Habitación no disponible; intento de cambiar habitación a una reserva; duplicidad en activos. |
| **`500`** | Error no contemplado de servidor o fallo inesperado de infraestructura. | Excepción no capturada en tiempo de ejecución. |
| **`503`** | Fallo de disponibilidad en comunicación síncrona entre microservicios. | Microservicio de Huéspedes inactivo al intentar generar una reserva vía Feign. |

---

## 7. Plan de Trabajo Scrum (Sprint de 3 Días)

El desarrollo del backend se distribuye equitativamente entre los ingenieros de backend:

```text
Día 1: Cimientos y Catálogos Independientes
├─ Draken: Creación de usuario y DDL Oracle + Microservicio de Huéspedes completo (CRUD, unicidad, borrado lógico).
└─ Lalo:   Microservicio de Habitaciones completo (CRUD, unicidad, máquina de estados manual PATCH).

Día 2: Orquestador y Reglas Transaccionales
├─ Draken: Microservicio de Reservas (Configuración Feign, validaciones cruzadas, creación POST y rollback compensatorio).
└─ Lalo:   Microservicio de Reservas (Máquina de estados de reservas PATCH, edición PUT de fechas y eliminación DELETE lógica).

Día 3: Seguridad, Reportes y Aseguramiento de Calidad
├─ Draken: Endpoints de Reportes (/historico, /por-estado) + Configuración Resource Server JWT en Huéspedes y Reservas.
└─ Lalo:   Configuración Resource Server JWT en Habitaciones + Ejecución y certificación de los 25 casos de prueba obligatorios.
```

---

## 8. Casos de Prueba Críticos de Aceptación

Para certificar la entrega técnica, el sistema debe responder satisfactoriamente a los 25 escenarios obligatorios evaluados a través del Gateway:

| # | Módulo | Caso de Prueba | Resultado Esperado |
| :-: | :--- | :--- | :---: |
| 01 | Reservas | Crear reserva con habitación `DISPONIBLE` | `201 OK`, habitación pasa a `OCUPADA` |
| 02 | Reservas | Crear reserva con habitación `OCUPADA` | `409 Conflict` |
| 03 | Reservas | Crear reserva con habitación en `LIMPIEZA` o `MANTENIMIENTO` | `409 Conflict` |
| 04 | Reservas | Check-in sobre reserva `CONFIRMADA` | `200 OK`, habitación sigue `OCUPADA` |
| 05 | Reservas | Check-out sobre reserva `EN_CURSO` | `200 OK`, habitación pasa a `LIMPIEZA` |
| 06 | Reservas | Check-out sobre reserva en estado `CONFIRMADA` | `409 Conflict` |
| 07 | Reservas | Cancelar reserva `CONFIRMADA` | `200 OK`, habitación pasa a `DISPONIBLE` |
| 08 | Reservas | Cancelar reserva `EN_CURSO` | `409 Conflict` |
| 09 | Reservas | Modificar fechas antes del check-in | `200 OK`, recalcula `precioTotal` |
| 10 | Reservas | Modificar fecha de entrada después del check-in | `409 Conflict` |
| 11 | Reservas | Modificar fecha de salida después del check-in ($\text{salida} > \text{entrada}$) | `200 OK`, recalcula `precioTotal` |
| 12 | Reservas | Modificar habitación o huésped en cualquier estado | `409 Conflict` |
| 13 | Reservas | Cálculo y recálculo automático de `precioTotal` al variar noches | `200 OK` (noches $\times$ precioNoche fijado) |
| 14 | Reservas | Eliminar una reserva `CONFIRMADA` o `EN_CURSO` | `409 Conflict` |
| 15 | Habitaciones | Eliminar habitación en estado `DISPONIBLE` | `200 OK` (Borrado lógico) |
| 16 | Habitaciones | Eliminar habitación en estado `OCUPADA` | `409 Conflict` |
| 17 | Habitaciones | ADMIN cambia estado manual de `LIMPIEZA` a `DISPONIBLE` | `200 OK` |
| 18 | Habitaciones | ADMIN intenta cambiar manualmente estado de habitación `OCUPADA` | `409 Conflict` |
| 19 | Habitaciones | USER intenta realizar cambio de estado manual | `403 Forbidden` |
| 20 | Huéspedes | Registrar o actualizar con Email duplicado de un registro `ACTIVO` | `409 Conflict` |
| 21 | Huéspedes | Registrar con Email que pertenecía a un registro `ELIMINADO` | `201 OK` (Permitido) |
| 22 | Huéspedes | Enviar Email con mayúsculas en el payload | `201 OK` (Se normaliza y guarda en minúsculas) |
| 23 | Huéspedes | Eliminar huésped con reserva `CONFIRMADA` o `EN_CURSO` | `409 Conflict` |
| 24 | Reportes | Consultar reportes con rol `USER` | `403 Forbidden` |
| 25 | Reportes | Consultar reportes con rol `ADMIN` | `200 OK` (Incluye registros cancelados y eliminados lógicos) |

---

## 9. Configuración de Variables de Entorno (`.env`)

Cada microservicio de dominio —**Huéspedes**, **Habitaciones** y **Reservas**— debe tener su propio archivo `.env` en la raíz del proyecto del servicio. El archivo contiene la configuración específica de su puerto, conexión a Oracle, URL base, ruta del Gateway y servidor emisor de tokens.

Plantilla para cada servicio:

```dotenv
PORT=Puerto
DB_URL=jdbc:oracle:thin:@//localhost:1521/FREE
DB_USER=NombreDB
DB_PASS=NombreDB

BASE_URL=http://localhost:Puerto
GATEWAY_URL=http://localhost:8090/api/NombreDeServicioActual

ISSUER_URI=http://localhost:9000
```

### Significado de las variables

| Variable | Descripción |
| :--- | :--- |
| `PORT` | Puerto local en el que se ejecuta el microservicio. |
| `DB_URL` | URL JDBC de conexión a Oracle Database. |
| `DB_USER` | Usuario de la base de datos. |
| `DB_PASS` | Contraseña del usuario de la base de datos. |
| `BASE_URL` | URL base del microservicio, usando el puerto asignado. |
| `GATEWAY_URL` | URL del Gateway correspondiente al servicio actual, con su prefijo `/api/`. |
| `ISSUER_URI` | URL del Authorization Server que emite los JWT. |

### Valores por microservicio

Sustituye los valores de ejemplo por los correspondientes a cada servicio:

| Microservicio | `PORT` de ejemplo | `BASE_URL` de ejemplo | `GATEWAY_URL` de ejemplo |
| :--- | :---: | :--- | :--- |
| Huéspedes | `8081` | `http://localhost:8081` | `http://localhost:8090/api/huespedes` |
| Habitaciones | `8082` | `http://localhost:8082` | `http://localhost:8090/api/habitaciones` |
| Reservas | `8083` | `http://localhost:8083` | `http://localhost:8090/api/reservas` |

**Importante:** `Puerto`, `NombreDB` y `NombreDeServicioActual` son marcadores de posición; reemplázalos por valores reales. Guarda las credenciales en los archivos `.env` locales y no los subas al repositorio. El puerto del Gateway indicado en `GATEWAY_URL` debe coincidir con la configuración real del proyecto; en la arquitectura documentada anteriormente aparece el Gateway en `8080`, mientras que la plantilla solicitada usa `8090`.

---

## 10. Despliegue y Ejecución Local

### Orden de Inicialización
1. **Oracle Database Container:** Levantar instancia en el puerto `1521` y ejecutar el script DDL de creación de tablas y usuario.
2. **Authorization Server:** Iniciar servidor de seguridad en el puerto asignado (emisor de tokens JWT).
3. **Eureka Server:** Iniciar en puerto `8761`. Verificar en `http://localhost:8761` que el panel esté en línea.
4. **API Gateway:** Iniciar en puerto `8080`.
5. **Microservicios de Dominio:** Iniciar `MS Huéspedes`, `MS Habitaciones` y `MS Reservas`. Verificar su registro en Eureka.
6. **Frontend Angular:** Levantar la aplicación web cliente para interactuar a través del Gateway.