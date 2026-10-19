package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.util.Domicilio;
import persistencia.AsociadoDTO;
import persistencia.BaseDeDatos;
import persistencia.DataAccessObject;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersistenciaIntegracionTest {

    private BaseDeDatos bd;
    private DataAccessObject dao;

    @BeforeEach
    public void setUp() {
        // Obtenemos la instancia Singleton y reiniciamos la BD a su estado semilla antes de cada prueba
        bd = BaseDeDatos.getInstancia();
        bd.iniciarBD();
        dao = new DataAccessObject();
    }

    // =========================================================================
    // 1. PATRÓN SINGLETON Y CONEXIÓN
    // =========================================================================
    @Test
    @Order(1)
    public void testPatronSingleton() {
        BaseDeDatos otraInstancia = BaseDeDatos.getInstancia();
        assertNotNull(bd.getConexion(), "La conexión no debería ser nula al inicializar el Singleton.");
        assertSame(bd, otraInstancia, "getInstancia() debe devolver exactamente la misma instancia (Singleton).");
    }

    // =========================================================================
    // 2. INICIALIZACIÓN DE TABLAS Y DATOS SEMILLA
    // =========================================================================
    @Test
    @Order(2)
    public void testInicializacionYDatosSemilla() throws SQLException {
        ArrayList<AsociadoDTO> lista = dao.cargarBD();
        assertNotNull(lista, "La lista devuelta no debe ser nula.");
        assertEquals(20, lista.size(), "iniciarBD() debe poblar la tabla exactamente con los 20 asociados semilla.");
        
        // Verificamos el mapeo del primer registro semilla (Juan State)
        AsociadoDTO primero = lista.get(0);
        assertEquals("Juan", primero.getNombre());
        assertEquals("State", primero.getApellido());
        assertEquals("12345678", primero.getDni());
    }

    // =========================================================================
    // 3. OPERACIONES DE CARGA (READ): Lotes acotados e individual
    // =========================================================================
    @Test
    @Order(3)
    public void testCargarConLimiteValido() throws SQLException {
        // Clase de equivalencia válida: límite > 0 (ej. 5)
        ArrayList<AsociadoDTO> lote = dao.cargarConLimite(5);
        assertEquals(5, lote.size(), "Debe recuperar exactamente la cantidad indicada en el límite.");
    }

    @Test
    @Order(4)
    public void testCargarConLimiteInvalido() {
        // Valor límite inválido: 0 (Precondición: limite > 0)
        assertThrows(AssertionError.class, () -> {
            dao.cargarConLimite(0);
        }, "Debe lanzar AssertionError si el límite es menor o igual a 0.");
    }

    @Test
    @Order(5)
    public void testCargarIndividualPorId() throws SQLException {
        // ESTE TEST DETECTARÁ EL BUG EN DataAccessObject.cargar(int id) (falta resultado.next())
        AsociadoDTO recuperado = dao.cargar(1);
        assertNotNull(recuperado, "Debe recuperar el asociado con ID 1.");
        assertEquals("12345678", recuperado.getDni(), "El DNI del asociado con ID 1 debe ser 12345678.");
    }

    // =========================================================================
    // 4. OPERACIÓN DE GUARDADO (CREATE) Y RESTRICCIONES SQL
    // =========================================================================
    @Test
    @Order(6)
    public void testGuardarAsociadoValido() throws SQLException {
        Domicilio dom = new Domicilio("Mar del Plata", 100, " Alem");
        Asociado nuevo = new Asociado("99887766", "223999888", " Codd", "Edgar", dom);

        dao.guardar(nuevo);

        ArrayList<AsociadoDTO> todos = dao.cargarBD();
        assertEquals(21, todos.size(), "El total de asociados debe incrementar a 21 tras insertar.");
        
        boolean encontrado = todos.stream().anyMatch(a -> a.getDni().equals("99887766") && a.getNombre().equals("Edgar"));
        assertTrue(encontrado, "El nuevo asociado debe encontrarse persistido en la base de datos.");
    }

    @Test
    @Order(7)
    public void testGuardarAsociadoDniDuplicadoLanzaSQLException() {
        // El DNI '12345678' ya existe en los datos semilla (Juan State). 
        // La columna DNI es UNIQUE en MySQL, por lo que debe lanzar SQLException.
        Domicilio dom = new Domicilio("Mar del Plata", 123, "Calle Falsa");
        Asociado duplicado = new Asociado("12345678", "223000000", "Perez", "Clon", dom);

        assertThrows(SQLException.class, () -> {
            dao.guardar(duplicado);
        }, "Debe lanzar SQLException al intentar insertar un DNI que ya existe en la tabla.");
    }

    @Test
    @Order(8)
    public void testGuardarAsociadoNuloLanzaError() {
        assertThrows(AssertionError.class, () -> {
            dao.guardar(null);
        }, "Guardar un asociado nulo debe ser rechazado por la precondición.");
    }

    // =========================================================================
    // 5. OPERACIÓN DE ACTUALIZACIÓN (UPDATE)
    // =========================================================================
    @Test
    @Order(9)
    public void testActualizarAsociadoExistente() throws SQLException {
        // ESTE TEST DETECTARÁ EL BUG DE ACTUALIZACIÓN (WHERE id = 1 hardcodeado en BaseDeDatos)
        // Intentamos modificar a Maria Template (ID 2, DNI '87654321') cambiando su calle y teléfono
        Domicilio nuevoDom = new Domicilio("Mar del Plata", 999, "Calle Modificada");
        Asociado mariaModificada = new Asociado("87654321", "2231111111", "Template", "Maria", nuevoDom);

        dao.actualizar(mariaModificada);

        // Buscamos a Maria por su DNI en la base de datos para verificar el cambio
        ArrayList<AsociadoDTO> todos = dao.cargarBD();
        AsociadoDTO mariaEnBD = todos.stream()
                .filter(a -> a.getDni().equals("87654321"))
                .findFirst()
                .orElse(null);

        assertNotNull(mariaEnBD, "Maria debe seguir existiendo en la BD.");
        assertEquals("Calle Modificada", mariaEnBD.getCalle(), "La calle de Maria debería haberse actualizado.");
        assertEquals(999, mariaEnBD.getNumero(), "El número de calle de Maria debería haberse actualizado.");
    }

    // =========================================================================
    // 6. OPERACIÓN DE ELIMINACIÓN (DELETE)
    // =========================================================================
    @Test
    @Order(10)
    public void testEliminarAsociadoPorDni() throws SQLException {
        // Eliminamos a Juan State (DNI '12345678')
        dao.eliminar("12345678");

        ArrayList<AsociadoDTO> todos = dao.cargarBD();
        assertEquals(19, todos.size(), "Deben quedar 19 asociados tras eliminar uno.");
        boolean existe = todos.stream().anyMatch(a -> a.getDni().equals("12345678"));
        assertFalse(existe, "El asociado con DNI 12345678 ya no debe estar en la base de datos.");
    }

    @Test
    @Order(11)
    public void testEliminarConDniVacioLanzaError() {
        assertThrows(AssertionError.class, () -> {
            dao.eliminar("");
        }, "No debe permitir eliminar pasando un DNI vacío.");
    }
}