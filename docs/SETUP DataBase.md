# SETUP DataBase

Guía para crear y configurar la base de datos del proyecto **Hotel** con
Oracle Database.

## 1. Requisitos previos

Antes de comenzar, verifica que cuentas con:

-   Oracle Database en ejecución.
-   Oracle SQL Developer u otra herramienta compatible.
-   Un usuario administrador de Oracle, como `SYS` o `SYSTEM`, con
    permisos para crear usuarios.
-   Acceso al archivo SQL del proyecto:
    `docs/recrusos/BD_de_reservas_hoteleras.sql`.
-   Docker, únicamente si Oracle Database se ejecuta en un contenedor.

> **Importante:** Los nombres de servicio, puertos y tablespaces pueden
> variar según la instalación. Los ejemplos siguientes utilizan
> `FREEPDB1`, el puerto `1521` y el tablespace `USERS`.

## 2. Crear el usuario de la base de datos

Conéctate a la base de datos o PDB de destino con un usuario
administrador. Si utilizas Oracle Database Free, normalmente será
`FREEPDB1`.

Ejecuta:

``` sql
alter session set "_oracle_script"=true;
create user hotel identified by hotel;
GRANT ALL PRIVILEGES TO hotel;
COMMIT;
```

La contraseña `hotel` es solo un valor de ejemplo. En entornos
compartidos o de producción, utiliza una contraseña segura y evita
guardar credenciales reales en el repositorio.

> **Nota:** Estos permisos permiten iniciar sesión y crear los objetos
> habituales de este esquema. Si el proyecto requiere otros objetos o
> privilegios, asígnalos de acuerdo con las necesidades reales y el
> principio de mínimo privilegio. El tablespace `USERS` debe existir; si
> tu instalación utiliza otro, ajusta el comando de cuota.

## 3. Configurar la conexión en Oracle SQL Developer

Crea una conexión nueva con los siguientes valores:

  Parámetro            Valor
  -------------------- ---------------------------------
  Nombre de conexión   `hotel`
  Tipo de conexión     `Basic`
  Hostname             `localhost`
  Puerto               `1521`
  Service name         `FREEPDB1`
  Usuario              `hotel`
  Contraseña           La definida al crear el usuario

Si Oracle está en Docker, confirma que el puerto del contenedor esté
publicado en el host y utiliza el puerto correspondiente. El hostname y
el nombre del servicio pueden variar.

Pulsa **Test** para validar la conexión y, si la prueba es exitosa,
guarda la configuración.

## 4. Crear las tablas

El script de definición de la base de datos se encuentra en:

``` text
./docs/recrusos/BD_de_reservas_hoteleras.sql
```

Ejecuta estos pasos:

1.  Abre el archivo `BD_de_reservas_hoteleras.sql` en Oracle SQL
    Developer.
2.  Selecciona la conexión del usuario `hotel`.
3.  Ejecuta el script completo mediante **Run Script** (normalmente,
    `F5`).
4.  Revisa el panel de salida y confirma que no se hayan producido
    errores.

El script crea las siguientes tablas:

-   **`HUESPED`**: información personal y documental de los huéspedes.
-   **`HABITACION`**: número, tipo, precio, capacidad y estadoHabitacion de las
    habitaciones.
-   **`RESERVA`**: fechas de estancia, huésped, habitación, número de
    personas y precios de la reserva.

Las tablas utilizan claves primarias, columnas de identidad y
restricciones `CHECK` para validar determinados valores, como el formato
del teléfono, los estados de registro, los precios y las fechas de
reserva.

La tabla `RESERVA` contiene las columnas `ID_HUESPED` e `ID_HABITACION`,
pero el script compartido **no define claves foráneas** para
relacionarlas con `HUESPED` y `HABITACION`. Si se requiere integridad
referencial en la base de datos, deben añadirse las restricciones
`FOREIGN KEY` correspondientes.

> **Importante:** El script compartido crea la estructura de las tablas,
> pero no contiene sentencias `INSERT`; por tanto, no carga datos
> iniciales.

## 5. Verificar la creación

Conéctate como `hotel` y ejecuta esta consulta para comprobar que las
tablas existen:

``` sql
SELECT TABLE_NAME
FROM USER_TABLES
WHERE TABLE_NAME IN ('HUESPED', 'HABITACION', 'RESERVA')
ORDER BY TABLE_NAME;
```

Deben aparecer las tres tablas: `HABITACION`, `HUESPED` y `RESERVA`.

También puedes revisar sus columnas desde el navegador de objetos de SQL
Developer, en **Tables**.

## 6. Diagrama entidad-relación

El diagrama entidad-relación (E-R) permite consultar visualmente las
entidades y sus relaciones.

-   [Consultar o publicar el diagrama en
    dbdiagram.io](https://dbdiagram.io/d/6859837df039ec6d36824cad)

Sustituye este enlace por la URL específica del diagrama del proyecto
cuando esté disponible.

## 7. Estructura de archivos

La ubicación del script SQL indicada para este proyecto es:

``` text
proyecto/
└── docs/
    └── recrusos/
        └── BD_de_reservas_hoteleras.sql
```

-   `docs/recrusos/BD_de_reservas_hoteleras.sql`: crea las tablas de la
    base de datos.

Si se agregan scripts de carga inicial o de datos de prueba, documenta
sus rutas y el orden en que deben ejecutarse. Ejecuta primero la
creación de tablas y después los scripts de inserción que correspondan.

## 8. Lista de verificación

Antes de dar por terminado el proceso, confirma lo siguiente:

-   [ ] Oracle Database está en ejecución y se conoce el servicio
    correcto.
-   [ ] El usuario `hotel` fue creado en la base de datos o PDB de
    destino.
-   [ ] La conexión de SQL Developer funciona.
-   [ ] El script `BD_de_reservas_hoteleras.sql` se ejecutó sin errores.
-   [ ] Existen las tablas `HUESPED`, `HABITACION` y `RESERVA`.
-   [ ] Se ejecutaron los scripts de carga de datos, si el proyecto
    dispone de ellos.
-   [ ] El enlace al diagrama E-R apunta al diagrama correcto.

Con estos pasos, el esquema de la base de datos queda creado y listo
para continuar con la configuración e integración del proyecto.
