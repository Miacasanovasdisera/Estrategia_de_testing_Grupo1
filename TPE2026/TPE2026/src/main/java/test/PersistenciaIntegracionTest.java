package test;
import org.junit.jupiter.api.*;

import persistencia.AsociadoDTO;
import persistencia.BaseDeDatos;
import persistencia.DataAccessObject;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersistenciaIntegracionTest {

    private BaseDeDatos bd;
    private DataAccessObject dao;

    @BeforeEach
    public void setUp() {
        // Ejecución del patrón Singleton para la conexión[cite: 4]
        bd = BaseDeDatos.getInstancia(); 
        dao = new DataAccessObject();
    }

    @Test
    @Order(1)
    public void testSingletonConnection() {
        BaseDeDatos otraBd = BaseDeDatos.getInstancia();
        assertSame(bd, otraBd, "El patrón Singleton no está devolviendo la misma instancia");
    }

    @Test
    @Order(2)
    public void testInicializacionEsquemaYDatosSemilla() {
        // Verifica que iniciarBD crea/recrea la tabla y puebla registros[cite: 4]
        bd.iniciarBD();
        assertFalse(dao.obtenerTodos().isEmpty(), "La base de datos debería contener datos semilla tras inicializarse");
    }

    @Test
    @Order(3)
    public void testOperacionesCRUDSql() {
        AsociadoDTO nuevo = new AsociadoDTO("Ana", "Gomez", "12345678", "Mar del Plata", "Funes", 1500, "223456789");
        
        // Create: Validar inserción SQL persistente[cite: 4]
        boolean insertado = dao.guardar(nuevo);
        assertTrue(insertado);

        // Read: Recuperar por identificador único (DNI)[cite: 4]
        AsociadoDTO recuperado = dao.buscarPorDni("12345678");
        assertNotNull(recuperado);
        assertEquals("Ana", recuperado.getNombre());

        // Update: Modificar registro existente[cite: 4]
        recuperado.setNumero(2000);
        boolean actualizado = dao.actualizar(recuperado);
        assertTrue(actualizado);
        assertEquals(2000, dao.buscarPorDni("12345678").getNumero());

        // Delete: Eliminar basándose en DNI[cite: 4]
        boolean eliminado = dao.eliminar("12345678");
        assertTrue(eliminado);
        assertNull(dao.buscarPorDni("12345678"));
    }
    
    @Test
    @Order(4)
    public void testPrevencionInyeccionSQL() {
        // Prueba de resiliencia con parámetros maliciosos[cite: 4]
        AsociadoDTO malicioso = new AsociadoDTO("Hack", "Drop", "1' OR '1'='1", "X", "Y", 1, "0");
        dao.guardar(malicioso);
        
        AsociadoDTO recuperado = dao.buscarPorDni("1' OR '1'='1");
        assertNotNull(recuperado, "El PreparedStatement debe tratar las comillas como literales, no como comandos SQL");
    }
}