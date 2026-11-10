# Informe de Estrategia de Testing: Aplicación Clínica
**Trabajo Práctico Final — Taller de Programación / Estrategia de Testing**  
**Grupo:** Grupo 1  
**Fecha:** Octubre 2026  

---

## 1. Presentación y Objetivos

El propósito de este informe es documentar la estrategia formal de aseguramiento de calidad y testing implementada sobre el sistema de gestión y simulación de la **Clínica**. 

La arquitectura de la aplicación exige validar el software en **tres niveles arquitectónicos bien diferenciados**:
1. **Testing Unitario Atómico (Componentes Independientes):** Comprobación en aislamiento estricto de la lógica interna, asignación, recuperación y validación de precondiciones en objetos de transferencia de datos ([`AsociadoDTO`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/AsociadoDTO.java)).
2. **Testing de Integración con Recursos Externos:** Verificación de la capa de persistencia real acoplada al motor de base de datos MySQL ([`BaseDeDatos`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/BaseDeDatos.java), [`DataAccessObject`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/DataAccessObject.java)), comprobando el patrón Singleton, consistencia transaccional y operaciones CRUD mediante consultas parametrizadas.
3. **Testing de Integración y Concurrencia con Dobles de Prueba (Mocks):** Verificación de la máquina de estados desacoplada ([`Ambulancia`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/Ambulancia.java), interfaz [`State`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/State.java)) mediante mocks para validar transiciones atómicas, junto con pruebas de estrés concurrente y sincronización multihilo sobre la rutina periódica ([`RetornoAutomatico`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/RetornoAutomatico.java)).

---

## 2. Punto 1: Testing Unitario de `AsociadoDTO`

### 2.1. Alcance y Estrategia
El componente [`AsociadoDTO`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/AsociadoDTO.java) encapsula los atributos del asociado y protege su integridad mediante precondiciones (`assert`). La suite de pruebas debe ejecutarse con aserciones habilitadas de la JVM (`-ea`) y sin levantar servicios externos.

### 2.2. Partición en Clases de Equivalencia y Análisis de Valores Límite (BVA)

* **Atributos de Texto (`nombre`, `apellido`, `dni`, `ciudad`, `calle`, `telefono`):**
  * **Regla:** Ningún campo de texto puede ser nulo (`assert campo != null`).
  * **Clase de Equivalencia Válida (CE_V):** Cadena de caracteres no nula (`String != null`).
  * **Clase de Equivalencia Inválida (CE_I):** Referencia nula (`null`). Debe disparar `AssertionError`.
* **Atributo Numérico (`numero` de domicilio):**
  * **Regla:** El número debe ser mayor o igual a cero (`assert numero >= 0`).
  * **Clase de Equivalencia Válida (CE_V1):** Entero $\ge 0$.
  * **Clase de Equivalencia Inválida (CE_I1):** Entero $< 0$.
  * **Valores Límite (BVA):**
    * Límite inferior válido: `0` (frontera exacta permitida).
    * Interior de la clase válida: `1` o valores positivos típicos.
    * Límite superior inválido: `-1` (frontera inmediata de rechazo).

### 2.3. Matriz de Casos de Prueba — Suite `AsociadoDTOTest`

| ID | Método Evaluado | Precondición | Entrada | Resultado Esperado (Oráculo) | Criterio de Selección |
|---|---|---|---|---|---|
| **CP_DTO_01** | `setNombre(String)` | DTO instanciado | `"Juan"` | `getNombre() == "Juan"` | CE_V: Texto válido |
| **CP_DTO_02** | `setNombre(String)` | DTO instanciado | `null` | Lanza `AssertionError` | CE_I: Texto nulo |
| **CP_DTO_03** | `setApellido(String)` | DTO instanciado | `"Casanovas"` | `getApellido() == "Casanovas"` | CE_V: Texto válido |
| **CP_DTO_04** | `setApellido(String)` | DTO instanciado | `null` | Lanza `AssertionError` | CE_I: Texto nulo |
| **CP_DTO_05** | `setDni(String)` | DTO instanciado | `"12345678"` | `getDni() == "12345678"` | CE_V: DNI no nulo |
| **CP_DTO_06** | `setDni(String)` | DTO instanciado | `null` | Lanza `AssertionError` | CE_I: DNI nulo |
| **CP_DTO_07** | `setCiudad(String)` | DTO instanciado | `"Mar del Plata"` | `getCiudad() == "Mar del Plata"` | CE_V: Ciudad válida |
| **CP_DTO_08** | `setCiudad(String)` | DTO instanciado | `null` | Lanza `AssertionError` | CE_I: Ciudad nula |
| **CP_DTO_09** | `setCalle(String)` | DTO instanciado | `"Av. Colón"` | `getCalle() == "Av. Colón"` | CE_V: Calle válida |
| **CP_DTO_10** | `setCalle(String)` | DTO instanciado | `null` | Lanza `AssertionError` | CE_I: Calle nula |
| **CP_DTO_11** | `setTelefono(String)` | DTO instanciado | `"2231234567"` | `getTelefono() == "2231234567"` | CE_V: Teléfono válido |
| **CP_DTO_12** | `setTelefono(String)` | DTO instanciado | `null` | Lanza `AssertionError` | CE_I: Teléfono nulo |
| **CP_DTO_13** | `setNumero(int)` | DTO instanciado | `0` | `getNumero() == 0` | BVA: Límite inferior válido |
| **CP_DTO_14** | `setNumero(int)` | DTO instanciado | `1` | `getNumero() == 1` | CE_V: Entero positivo |
| **CP_DTO_15** | `setNumero(int)` | DTO instanciado | `-1` | Lanza `AssertionError` | BVA: Límite inmediato inválido |

---

## 3. Punto 2: Testing de Integración de Persistencia

### 3.1. Alcance y Entorno
Valida la comunicación real entre [`DataAccessObject`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/DataAccessObject.java) y [`BaseDeDatos`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/BaseDeDatos.java) contra una instancia local de MySQL. Se verifica el ciclo de vida de la conexión única (Singleton), la creación de esquema, el reinicio de datos semilla (20 asociados) y el manejo de restricciones relacionales (clave primaria, unicidad de DNI).

### 3.2. Clases de Equivalencia y Valores Límite

* **Operación `cargarConLimite(int limite)`:**
  * Precondición: `assert limite > 0`.
  * **CE_V:** Límite entero positivo.
  * **CE_I:** Límite menor o igual a cero.
  * **BVA:** Límite válido frontera = `1`; Límite inválido frontera = `0`.
* **Operación `cargar(int id)`:**
  * Precondición: `assert id > 0`.
  * **BVA:** ID válido inicial = `1`; ID inválido frontera = `0`.
* **Operación `guardar(Asociado asociado)`:**
  * **CE_V:** Asociado con datos válidos y DNI no existente en la BD.
  * **CE_I1 (Precondición):** Parámetro `null` $\rightarrow$ Lanza `AssertionError`.
  * **CE_I2 (Integridad SQL):** DNI ya existente en tabla `Asociados` (columna `UNIQUE`) $\rightarrow$ Lanza `SQLException`.
* **Operación `eliminar(String dni)`:**
  * **CE_V:** DNI no vacío existente.
  * **CE_I:** Cadena vacía `""` $\rightarrow$ Lanza `AssertionError`.

### 3.3. Matriz de Casos de Prueba — Suite `PersistenciaIntegracionTest`

| ID | Método / Flujo | Precondición | Entrada | Resultado Esperado | Criterio de Selección |
|---|---|---|---|---|---|
| **CP_INT_01** | `BaseDeDatos.getInstancia()` | Conexión activa | N/A | Misma referencia de memoria (`assertSame`) y conexión no nula | Singleton |
| **CP_INT_02** | `DataAccessObject.cargarBD()` | `iniciarBD()` ejecutado | N/A | Lista con 20 asociados semilla. Primer registro: Juan State, DNI 12345678 | Inicialización y seed |
| **CP_INT_03** | `cargarConLimite(int)` | BD inicializada | `limite = 1` | Retorna lista con exactamente 1 elemento | BVA: Límite válido frontera |
| **CP_INT_04** | `cargarConLimite(int)` | BD inicializada | `limite = 0` | Lanza `AssertionError` | BVA: Límite inválido frontera |
| **CP_INT_05** | `cargar(int)` | BD inicializada | `id = 1` | DTO no nulo con DNI `"12345678"` | Recuperación puntual por ID |
| **CP_INT_06** | `cargar(int)` | BD inicializada | `id = 0` | Lanza `AssertionError` | BVA: ID frontera inválida |
| **CP_INT_07** | `guardar(Asociado)` | 20 registros | Edgar Codd, DNI `"99887766"` | Total aumenta a 21 registros. Datos persistidos correctamente | Inserción CRUD válida |
| **CP_INT_08** | `guardar(Asociado)` | DNI `"12345678"` ya existe | Clon Perez, DNI `"12345678"` | Lanza `SQLException` por violación de clave única | Integridad relacional SQL |
| **CP_INT_09** | `guardar(Asociado)` | BD inicializada | `null` | Lanza `AssertionError` | CE_I: Objeto nulo |
| **CP_INT_10** | `actualizar(Asociado)` | Maria Template (ID 2, DNI `"87654321"`) | Maria con nueva calle y número `999` | Se actualizan los datos de María en la BD | **Detección de defecto (Bug)** |
| **CP_INT_11** | `eliminar(String)` | Juan State existe | `dni = "12345678"` | Total disminuye a 19 registros. Juan no existe más en BD | Eliminación CRUD |
| **CP_INT_12** | `eliminar(String)` | BD inicializada | `dni = ""` | Lanza `AssertionError` | Precondición DNI vacío |
| **CP_INT_13** | Mapeo Integral | DTO con campos disímiles | Domicilio `"Batan"`, calle `"Independencia"`, num `2450` | Todos los campos mapean a sus columnas exactas | Validación de asignación JDBC |

---

## 4. Punto 3: Testing de Integración con Mocks y Concurrencia

### 4.1. Alcance y Arquitectura de Dobles de Prueba
El subsistema de ambulancias implementa el patrón **State** acoplado a un modelo multihilo de recursos compartidos. Para probar este componente sin depender de la lógica concreta de cada estado ni de retardos estocásticos de la simulación, se diseñaron tres dobles de prueba específicos:
1. **`StateMock`:** Doble de prueba (Mock) thread-safe que implementa la interfaz [`State`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/State.java). Registra cronológicamente cada invocación (`SolicitudDeTraslado`, `SolicitudDeAtencionDomicilio`, `SolicitudMantenimiento`, `RetornoClinica`) y permite programar respuestas para simular estados futuros o faltantes.
2. **`ObservadorEspia`:** Doble de prueba (Spy) que implementa `Observer`, capturando y clasificando en memoria las instancias de `NotificacionSimulacion` emitidas por [`Ambulancia`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/Ambulancia.java).
3. **`AmbulanciaStub`:** Doble de prueba (Stub) que sobreescribe `isSimulacionActiva()` para devolver secuencias guionadas determinísticas al hilo [`RetornoAutomatico`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/RetornoAutomatico.java), aislando su ciclo de ejecución.

### 4.2. Oráculo de Transición: Tabla Completa de Estados (6 Estados $\times$ 4 Eventos = 24 Combinaciones)
Basado formalmente en el diagrama de estados del sistema ([`UML-State2.png`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/UML-State2.png)):

| Estado Origen | Solicitud Traslado | Solicitud Domicilio | Solicitud Mantenimiento | Retorno Clínica |
|---|---|---|---|---|
| **DisponibleState** | $\rightarrow$ TrasladandoPaciente | $\rightarrow$ AtendiendoDomicilio | $\rightarrow$ EnTaller | Permanece Disponible |
| **TrasladandoPacienteClinicaState** | Rechaza (Permanece) | Rechaza (Permanece) | Rechaza (Permanece) | **$\rightarrow$ DisponibleState** *(Oráculo UML)* |
| **AtendiendoEnDomicilioState** | Rechaza (Permanece) | Rechaza (Permanece) | Rechaza (Permanece) | $\rightarrow$ RegresandoClinicaSinPaciente |
| **RegresandoClinicaSinPacienteState**| $\rightarrow$ TrasladandoPaciente | $\rightarrow$ AtendiendoDomicilio | Rechaza (Permanece) | $\rightarrow$ DisponibleState |
| **EnTallerState** | Rechaza (Permanece) | Rechaza (Permanece) | $\rightarrow$ RegresandoDelTaller | Rechaza (Permanece) |
| **RegresandoDelTallerState** | Rechaza (Permanece) | Rechaza (Permanece) | Rechaza (Permanece) | $\rightarrow$ DisponibleState |

### 4.3. Matriz de Casos de Prueba — Mocks y Concurrencia

#### A. Suite Atómica con Mocks (`AmbulanciaMockTest`)
| ID | Método / Escenario | Precondición | Evento | Resultado Esperado |
|---|---|---|---|---|
| **CP_MCK_01** | `setState(State)` | Ambulancia nueva | Inyectar `StateMock` | `getEstadoActual()` devuelve la referencia exacta del mock |
| **CP_MCK_02** | `setState(null)` | Ambulancia nueva | Inyectar `null` | Lanza `AssertionError` por precondición |
| **CP_MCK_03** | `trasladarALaClinica()` | Ambulancia libre + Mock | Llamar con Asociado | Delega 1 vez en `SolicitudDeTraslado()`, ambulancia queda ocupada y notifica `"OK"` |
| **CP_MCK_04** | `atenderDomicilio()` | Ambulancia libre + Mock | Llamar con Asociado | Delega 1 vez en `SolicitudDeAtencionDomicilio()`, ambulancia ocupada y notifica `"OK"` |
| **CP_MCK_05** | `solicitarMantenimiento()` | Ambulancia libre + Mock | Llamar con Operario | Delega 1 vez en `SolicitudMantenimiento()`, ambulancia ocupada y notifica `"OK"` |
| **CP_MCK_06** | `volviendoDelTaller()` | Ambulancia ocupada + Mock | Llamar método | Delega en `SolicitudMantenimiento()` y libera la ambulancia (`isOcupado() == false`) |
| **CP_MCK_07** | `regresarSinPaciente()` | Ambulancia ocupada + Mock | Llamar método | Delega en `RetornoClinica()` y libera la ambulancia |
| **CP_MCK_08** | Invocación con `null` | Ambulancia + Mock | Parámetros nulos en traslados o mantenimientos | Lanza `AssertionError` y el mock registra cero llamadas |
| **CP_MCK_09** | Simulación inactiva | `isSimulacionActiva = false` | Disparar solicitudes | No delega llamadas al mock y no ocupa la ambulancia |
| **CP_MCK_10** | Exclusión mutua (Wait/Notify) | Ambulancia ocupada | Hilo solicita traslado | Hilo pasa a `WAITING`. Al ejecutar `regresarSinPaciente()`, se despierta y atiende |
| **CP_MCK_11** | Interrupción en espera | Hilo esperando en `wait()` | Interrumpir hilo | Hilo finaliza limpiamente preservando bandera de interrupción sin delegar al mock |

#### B. Suite de Integración y Concurrencia Real (`AmbulanciaIntegracionTest`)
| ID | Escenario | Precondición | Estímulo | Resultado Esperado |
|---|---|---|---|---|
| **CP_CONC_01** | Tabla de Transiciones (24 casos) | Cada estado de la máquina | Ejecutar cada uno de los 4 métodos de `State` | El estado resultante debe coincidir exactamente con la especificación formal del UML |
| **CP_CONC_02** | Dos traslados consecutivos | Ambulancia libre | 1° Traslado $\rightarrow$ Regreso $\rightarrow$ 2° Traslado | Ambulancia debe admitir el 2° traslado y quedar en `TrasladandoPacienteClinicaState` |
| **CP_CONC_03** | Exclusión mutua con 4 hilos concurrentes | 4 asociados enviando 12 solicitudes en paralelo | Carrera crítica sobre la ambulancia | Máximo de usuarios concurrentes en sección crítica = `1`. Atiende las 12 solicitudes |
| **CP_CONC_04** | Coherencia de estado bajo carga concurrente | Múltiples hilos compitiendo por traslados | Verificar estado durante el traslado | Cero discrepancias: el estado es siempre `TrasladandoPacienteClinicaState` |
| **CP_CONC_05** | Operario espera liberación | Ambulancia ocupada por paciente | Operario solicita mantenimiento | Operario se bloquea. Al retornar paciente, operario toma la ambulancia |
| **CP_CONC_06** | Fin de simulación libera bloqueos | 5 hilos en `wait()` | `setSimulacionActiva(false)` | Todos los hilos se desbloquean y finalizan sin mutar indebidamente el estado |
| **CP_CONC_07** | BVA: `verificarYFinalizarSimulacion()` | Asociados con solicitudes restantes = `0` | Invocar verificación | Simulación se marca como finalizada (`isSimulacionActiva == false`) |
| **CP_CONC_08** | BVA: `verificarYFinalizarSimulacion()` | 1 asociado con solicitudes restantes = `1` | Invocar verificación | Simulación continúa activa (`isSimulacionActiva == true`) |

#### C. Suite del Hilo Periódico (`RetornoAutomaticoTest`)
| ID | Escenario | Precondición | Ejecución | Resultado Esperado |
|---|---|---|---|---|
| **CP_RET_01** | Ejecución periódica | `AmbulanciaStub` activa (secuencia `true`, `true`, `false`) | Iniciar hilo `RetornoAutomatico` | El hilo ejecuta el retorno automático una vez por intervalo de 10 s y termina |
| **CP_RET_02** | Simulación inactiva | `AmbulanciaStub` con `isSimulacionActiva = false` | Iniciar hilo | El hilo finaliza de forma inmediata sin esperar 10 segundos ni efectuar retornos |
| **CP_RET_03** | Interrupción limpia | Hilo durmiendo en intervalo | `hilo.interrupt()` | El hilo captura `InterruptedException` y termina de inmediato sin invocar retornos |
| **CP_RET_04** | Integración del retorno automático | Ambulancia en `RegresandoDelTallerState` | `RetornoAutomatico` corriendo | Tras el intervalo, la ambulancia transiciona automáticamente a `DisponibleState` |

---

## 5. Matriz Resumen de Defectos Detectados en el Código Provisto

Las suites desarrolladas descubrieron formalmente **3 defectos críticos** introducidos en el código fuente base:

| ID Defecto | Módulo y Archivo | Línea | Causa Raíz (Defecto en Código Provisto) | Impacto / Falla Observada | Corrección Requerida |
|---|---|---|---|---|---|
| **BUG_01** | Persistencia: [`BaseDeDatos.java`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/BaseDeDatos.java#L118) | 118 | `stmt.setInt(8, 1);` se encuentra hardcodeado con valor constante `1` en lugar de utilizar el ID del asociado a modificar. | Cualquier intento de actualización sobre asociados con ID $\neq 1$ modifica erróneamente siempre al asociado con ID 1 (Juan State), corrompiendo datos existentes. | Extraer el ID correspondiente al asociado objetivo o vincular la condición del `UPDATE` al DNI único (`WHERE dni = ?`). |
| **BUG_02** | Concurrencia: [`RetornoAutomatico.java`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/RetornoAutomatico.java#L25) | 25 | Condición de ciclo invertida: `while (!ambulancia.isSimulacionActiva())`. | Cuando la simulación está **activa**, la condición evalúa a `false` de inmediato y el hilo finaliza instantáneamente sin jamás ejecutar ningún retorno automático. | Invertir la condición a `while (ambulancia.isSimulacionActiva())`. |
| **BUG_03** | Máquina de Estados: [`TrasladandoPacienteClinicaState.java`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/TrasladandoPacienteClinicaState.java#L56) | 56 | En `RetornoClinica()` transiciona erróneamente a `RegresandoDelTallerState` en vez de `DisponibleState`. | Una ambulancia que termina de trasladar un paciente queda en estado "regresando del taller", rechazando nuevas atenciones y perdiendo solicitudes subsecuentes. | Modificar la transición a: `this.ambulancia.setState(new DisponibleState(this.ambulancia));`. |

---

## 6. Resultados de Ejecución y Evidencia (Rojo y Verde)

Para dar estricto cumplimiento a los **Criterios de Entrega** del Trabajo Práctico, se deben incorporar al documento final las capturas del test runner de Eclipse / IntelliJ / JUnit Platform:

### 6.1. Ejecución en Rojo (Detección de Defectos)
* **Captura 1 (Punto 2 — Persistencia):**
  * Suite: `PersistenciaIntegracionTest`
  * Test fallido: `testActualizarAsociadoExistente()`
  * Mensaje: `AssertionFailedError: La calle de Maria debería haberse actualizado. Expected: Calle Modificada, Actual: Libertad`.
* **Captura 2 (Punto 3 — Concurrencia y Estados):**
  * Suite: `AmbulanciaIntegracionTest`
  * Tests fallidos: 
    * `testCicloCompletoTraslado()`: Discrepancia de tipo de estado esperado (`DisponibleState` vs `RegresandoDelTallerState`).
    * `testTransicion("Trasladando + retorno -> Disponible")`.
  * Suite: `RetornoAutomaticoTest`
  * Tests fallidos:
    * `testConSimulacionActivaEjecutaElRetorno()`: Se esperaban retornos pero se registraron 0 por el ciclo invertido.
    * `testConSimulacionInactivaTerminaInmediatamente()`.

### 6.2. Ejecución en Verde (Validación del Sistema Corregido)
* **Captura 3:** Ejecución limpia con **todos los tests en verde (100% exitosos)**:
  * `AsociadoDTOTest`: 15 tests pasados.
  * `PersistenciaIntegracionTest`: 13 tests pasados.
  * `AmbulanciaMockTest`: 13 tests pasados.
  * `AmbulanciaIntegracionTest`: 34 tests pasados (incluye los 24 casos parametrizados de la tabla de estados).
  * `RetornoAutomaticoTest`: 4 tests pasados.
  * **Total:** 79 tests ejecutados, 0 fallos, 0 errores.

---

## 7. Conclusiones Técnicas

1. **Aislamiento vs. Integración:**
   El testing atómico de [`AsociadoDTO`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/persistencia/AsociadoDTO.java) demostró que las validaciones de precondición en memoria son rápidas y confiables, pero no garantizan la persistencia. Por su parte, el testing de integración con MySQL expuso de inmediato fallas de diseño invisibles a nivel unitario, como parámetros desfasados o hardcodeados en sentencias JDBC preparadas.
2. **Utilidad de Dobles de Prueba en Sistemas Complejos:**
   La utilización de mocks para la interfaz [`State`](file:///c:/Users/gabri/Escritorio/TallerA/Estrategia_de_testing_Grupo1/TPE2026/TPE2026/src/main/java/modelo/modeloDominio/ambulancia/State.java) permitió desacoplar la verificación de la orquestación (`Ambulancia`) de la lógica interna de los estados concretos. Esto evitó retardos innecesarios (`sleep`) y permitió validar de forma determinística la exclusión mutua (`synchronized`) y las operaciones de sincronización (`wait` / `notifyAll`).
3. **Detección Temprana de Fallas de Concurrencia:**
   Los defectos de concurrencia (como ciclos invertidos o estados mal asignados) suelen manifestarse en producción como interbloqueos (deadlocks) o inanición (starvation) intermitentes. La implementación de pruebas con aserciones temporales acotadas (`@Timeout`) y herramientas determinísticas de sincronización (`CountDownLatch`, esperas activas con condición) garantiza una red de seguridad contra regresiones en entornos multiproceso.
