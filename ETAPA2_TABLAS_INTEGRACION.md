# Etapa 2: Testing de Integración (`AsociadoDTO`, `DataAccessObject` y `BaseDeDatos`)

---

## Definición Formal de Escenarios de Integración

### Escenario 1: Base de Datos inicializada con lote semilla (`@BeforeEach setUp`)
* **Infraestructura externa:** Motor de base de datos MySQL activo en `localhost:3306`.
* **Preparación previa a cada prueba:**
  1. Se obtiene la instancia única del manejador de conexión mediante `bd = BaseDeDatos.getInstancia()`.
  2. Se ejecuta `bd.iniciarBD()`, la cual crea el esquema `Grupo_7` (si no existe), elimina y vuelve a crear la tabla `Asociados` (`id INT AUTO_INCREMENT PRIMARY KEY`, `DNI VARCHAR(10) UNIQUE`, etc.) e inserta los **20 asociados semilla** (entre ellos: registro con `id = 1`, `"Juan State"`, DNI `"12345678"`; y registro con `id = 2`, `"Maria Template"`, DNI `"87654321"`).
  3. Se instancia el objeto de acceso a datos `dao = new DataAccessObject()`, acoplado a la instancia Singleton de `BaseDeDatos`.

---

## 1. Módulo `BaseDeDatos`

### *public static BaseDeDatos getInstancia()* y *public Connection getConexion()*

**Escenario:** Escenario 1 (Conexión abierta e instancia inicializada en `@BeforeEach`).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| Estado de la instancia estática `instancia` en `BaseDeDatos` | `instancia == NULL` (primera invocación, crea la conexión) (1)<br>`instancia != NULL` (invocaciones sucesivas, reutiliza la instancia) (2) | Servidor MySQL inalcanzable o credenciales inválidas (`conexion == NULL`) (3) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | Invocación a `BaseDeDatos.getInstancia()` y `bd.getConexion()` | Segunda llamada a `BaseDeDatos.getInstancia()` tras el `@BeforeEach` | `getConexion()` retorna un objeto `Connection` activo no nulo (`!= NULL`) contra MySQL y `getInstancia()` devuelve exactamente la misma referencia en memoria (`assertSame(bd, otraInstancia)`), cumpliendo el patrón Singleton. | 1, 2 |

---

### *public void iniciarBD()* (verificado en integración junto a *dao.cargarBD()*)

**Escenario:** Escenario 1 (Ejecución de `bd.iniciarBD()` al iniciar la prueba).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| Estado de la conexión JDBC en `BaseDeDatos` | `conexion != NULL` y activa contra MySQL (1) | `conexion == NULL` o cerrada (2) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | Estado de BD tras `iniciarBD()` | Ejecución de `iniciarBD()` seguida de `dao.cargarBD()` | MySQL recrea la tabla `Asociados` y la puebla automáticamente con los 20 registros semilla. `dao.cargarBD()` recupera una lista no nula de tamaño 20 donde el primer elemento (`id = 1`) se mapea a `AsociadoDTO` con nombre `"Juan"`, apellido `"State"` y DNI `"12345678"`. | 1 |

---

## 2. Módulo `DataAccessObject` (en integración con `AsociadoDTO` y `BaseDeDatos`)

### *public ArrayList\<AsociadoDTO\> cargarBD() throws SQLException*

**Escenario:** Escenario 1 (Tabla `Asociados` en MySQL con 20 registros semilla).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| Estado de la tabla `Asociados` en MySQL | Tabla `Asociados` creada y con registros cargados (1) | Tabla `Asociados` inexistente o conexión cerrada (2) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | Estado de tabla `Asociados` | 20 registros semilla cargados por `iniciarBD()` | `DAO` invoca `bd.ejecutarConsulta("SELECT * FROM Asociados ;", -1)`, recorre el `ResultSet`, transforma cada fila en un `AsociadoDTO` mediante `CrearDTO(...)` y devuelve un `ArrayList<AsociadoDTO>` con los 20 asociados. | 1 |

---

### *public ArrayList\<AsociadoDTO\> cargarConLimite(int limite) throws SQLException*

**Escenario:** Escenario 1 (Tabla `Asociados` en MySQL con 20 registros semilla).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| `limite` (Parámetro Java) | `limite > 0` (1) | `limite <= 0` (2) |
| Cantidad de registros en tabla MySQL `Asociados` | Tabla con al menos `limite` registros disponibles (3) | Tabla sin conexión / error SQL (4) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | `limite` | `1` *(Valor límite inferior válido)* | `DAO` pasa el parámetro a `BaseDeDatos.ejecutarConsulta("SELECT * FROM Asociados LIMIT ? ;", 1)`, MySQL acota la consulta y se retorna un `ArrayList<AsociadoDTO>` con exactamente 1 registro mapeado. | 1, 3 |
| 2 | `limite` | `0` *(Valor límite superior inválido)* | Falla en la precondición de `DAO` (`assert limite > 0 : "El limite debe ser mayor que 0";`) lanzando `AssertionError` antes de consultar a MySQL. | 2 |

---

### *public AsociadoDTO cargar(int id) throws SQLException*

**Escenario:** Escenario 1 (Tabla `Asociados` en MySQL con 20 registros semilla, IDs del 1 al 20).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| `id` (Parámetro Java) | `id > 0` (1) | `id <= 0` (2) |
| Existencia de `id` en tabla MySQL `Asociados` | El `id` **existe** como clave primaria en la tabla `Asociados` (3) | El `id` **no existe** en la tabla `Asociados` (`ResultSet` vacío o sin posicionar cursor) (4) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | `id` | `1` *(Valor límite inferior válido, corresponde a Juan State)* | `DAO` ejecuta `bd.ejecutarConsulta("SELECT * FROM Asociados WHERE id = ? ;", 1)` y retorna el `AsociadoDTO` con DNI `"12345678"`. *(Nota de integración: en el código provisto `cargar(int)` no invoca `resultado.next()` antes de leer el `ResultSet`, lo cual provoca `SQLException` en el driver MySQL al intentar extraer las columnas).* | 1, 3 |
| 2 | `id` | `0` *(Valor límite superior inválido)* | Falla en la precondición de `DAO` (`assert id > 0 : "El id debe ser mayor que 0";`) lanzando `AssertionError` sin ejecutar consulta en MySQL. | 2 |

---

### *public void guardar(Asociado asociado) throws SQLException*

**Escenario:** Escenario 1 (Tabla `Asociados` en MySQL con 20 registros semilla).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| `asociado` (Parámetro Java) | `asociado != NULL` (1) | `asociado == NULL` (2) |
| `asociado.dni` en tabla MySQL `Asociados` | DNI **no existe** en la tabla `Asociados` (3) | DNI **ya existe** en la tabla `Asociados` (viola restricción `UNIQUE`) (4) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | `asociado` | `Asociado("Edgar", "Codd", "99887766", "223999888", Domicilio("Alem", 100, "Mar del Plata"))` | `DAO` convierte la entidad a `AsociadoDTO`, `BaseDeDatos` ejecuta `ejecutarInsert` (`PreparedStatement`) y la tabla en MySQL pasa de 20 a 21 registros. | 1, 3 |
| 2 | `asociado` | `Asociado("Grace", "Hopper", "55443322", "2234567890", Domicilio("Independencia", 2450, "Batan"))` | El registro insertado en MySQL se recupera como `AsociadoDTO` con las 7 columnas (`nombre`, `apellido`, `dni`, `telefono`, `calle`, `numero`, `ciudad`) mapeadas en el orden exacto sin cruzarse. | 1, 3 |
| 3 | `asociado` | `Asociado("Clon", "Perez", "12345678", "223000000", Domicilio("Calle Falsa", 123, "Mar del Plata"))` *(DNI de Juan State ya existente en semilla)* | `DAO` crea el `AsociadoDTO` y `BaseDeDatos` ejecuta el `INSERT`, pero MySQL rechaza la operación y lanza `SQLException` por violación de clave `UNIQUE`. | 1, 4 |
| 4 | `asociado` | `NULL` | Falla en la precondición de `DAO` (`assert asociado != null;`) lanzando `AssertionError` sin tocar MySQL. | 2 |

---

### *public void actualizar(Asociado asociado) throws SQLException*

**Escenario:** Escenario 1 (Tabla `Asociados` en MySQL con 20 registros semilla, incluyendo a `"Maria Template"` con `id = 2`, DNI `"87654321"`, calle `"Libertad"`, número `742`).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| `asociado` (Parámetro Java) | `asociado != NULL` (1) | `asociado == NULL` (2) |
| Registro objetivo en tabla MySQL `Asociados` | El asociado a modificar **existe** en la tabla `Asociados` (3) | El asociado a modificar **no existe** en la tabla `Asociados` (4) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | `asociado` | `Asociado("Maria", "Template", "87654321", "2231111111", Domicilio("Calle Modificada", 999, "Mar del Plata"))` | `DAO` convierte a `AsociadoDTO` y `BaseDeDatos.ejecutarActualizacion` ejecuta el `UPDATE` en MySQL modificando el domicilio de Maria Template a `"Calle Modificada"` `999`. *(En el código provisto esta prueba **FALLA EN ROJO** y detecta el defecto de `stmt.setInt(8, 1)` hardcodeado en `BaseDeDatos.java`, que sobrescribe al `id = 1` en lugar del registro de María).* | 1, 3 |
| 2 | `asociado` | `NULL` | Falla en la precondición de `DAO` (`assert asociado != null;`) lanzando `AssertionError` sin ejecutar `UPDATE` en MySQL. | 2 |

---

### *public void eliminar(String dni) throws SQLException*

**Escenario:** Escenario 1 (Tabla `Asociados` en MySQL con 20 registros semilla, incluyendo a Juan State con DNI `"12345678"`).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| `dni` (Parámetro Java) | `dni != ""` y `dni != NULL` (1) | `dni == ""` (cadena vacía) (2)<br>`dni == NULL` (3) |
| Existencia de `dni` en tabla MySQL `Asociados` | El `dni` **existe** en la tabla `Asociados` (4) | El `dni` **no existe** en la tabla `Asociados` (5) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | `dni` | `"12345678"` *(DNI existente de Juan State)* | `DAO` invoca `bd.ejecutarDelete("DELETE FROM Asociados WHERE dni = ? ;", "12345678")`. MySQL elimina la fila, el total en `cargarBD()` disminuye de 20 a 19 registros y ya no existe ningún registro con DNI `"12345678"`. | 1, 4 |
| 2 | `dni` | `""` *(Cadena vacía)* | Falla en la precondición de `DAO` (`assert !Objects.equals(dni, "");`) lanzando `AssertionError` sin ejecutar `DELETE` en MySQL, conservando los 20 registros. | 2 |

---

## 3. Métodos de Mapeo en `DataAccessObject` con `AsociadoDTO`

### *public AsociadoDTO CrearDTO(Asociado asociado)* y *public AsociadoDTO CrearDTO(int id, String nombre, String apellido, String dni, String calle, int numero, String ciudad, String telefono)*

**Escenario:** Escenario 1 (Invocados internamente durante las operaciones de guardado, actualización y lectura contra MySQL).

**Tabla partición**

| Dato de entrada | Clase válida | Clase inválida |
|---|---|---|
| `asociado` / Campos leídos del `ResultSet` de MySQL | Objeto `Asociado != NULL` con atributos válidos (1)<br>Fila de `ResultSet` con columnas no nulas y `numero >= 0` (2) | `asociado == NULL` (3)<br>Algún atributo de texto `NULL` o `numero < 0` (viola setters de `AsociadoDTO`) (4) |

**Batería de pruebas**

| Número de prueba | Dato de entrada | Valor | Salida esperada (Integración) | Clases que alcanza |
|---|---|---|---|---|
| 1 | `asociado` (Ida y vuelta completa Java $\rightarrow$ MySQL $\rightarrow$ Java) | `Asociado("Grace", "Hopper", "55443322", "2234567890", Domicilio("Independencia", 2450, "Batan"))` | Al guardar, `CrearDTO(Asociado)` instancia un `AsociadoDTO` validando cada setter; al leer desde MySQL, `CrearDTO(id, ...)` reconstruye el `AsociadoDTO` desde el `ResultSet` conservando los 7 atributos en sus columnas exactas. | 1, 2 |

