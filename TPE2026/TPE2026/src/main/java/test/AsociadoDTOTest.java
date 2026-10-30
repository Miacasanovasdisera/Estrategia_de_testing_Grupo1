package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import persistencia.AsociadoDTO;

public class AsociadoDTOTest {

    private AsociadoDTO asociado;

    @BeforeEach
    public void setUp() {
        // Inicialización aislada en memoria previa a cada test
        asociado = new AsociadoDTO();
    }

    // ==========================================
    // TESTS PARA NOMBRE
    // ==========================================

    @Test
    public void testSetNombreValido() {
        asociado.setNombre("Juan");
        assertEquals("Juan", asociado.getNombre(), "El nombre asignado debe recuperarse correctamente.");
    }

    @Test
    public void testSetNombreNuloLanzaError() {
        assertThrows(AssertionError.class, () -> asociado.setNombre(null), 
                "Debe fallar con AssertionError al asignar un nombre nulo.");
    }

    // ==========================================
    // TESTS PARA APELLIDO
    // ==========================================

    @Test
    public void testSetApellidoValido() {
        asociado.setApellido("Casanovas");
        assertEquals("Casanovas", asociado.getApellido(), "El apellido asignado debe recuperarse correctamente.");
    }

    @Test
    public void testSetApellidoNuloLanzaError() {
        assertThrows(AssertionError.class, () -> asociado.setApellido(null), 
                "Debe fallar con AssertionError al asignar un apellido nulo.");
    }

    // ==========================================
    // TESTS PARA DNI
    // ==========================================

    @Test
    public void testSetDniValido() {
        asociado.setDni("12345678");
        assertEquals("12345678", asociado.getDni(), "El DNI asignado debe recuperarse correctamente.");
    }

    @Test
    public void testSetDniNuloLanzaError() {
        assertThrows(AssertionError.class, () -> asociado.setDni(null), 
                "Debe fallar con AssertionError al asignar un DNI nulo.");
    }

    // ==========================================
    // TESTS PARA CIUDAD
    // ==========================================

    @Test
    public void testSetCiudadValida() {
        asociado.setCiudad("Mar del Plata");
        assertEquals("Mar del Plata", asociado.getCiudad(), "La ciudad asignada debe recuperarse correctamente.");
    }

    @Test
    public void testSetCiudadNulaLanzaError() {
        assertThrows(AssertionError.class, () -> asociado.setCiudad(null), 
                "Debe fallar con AssertionError al asignar una ciudad nula.");
    }

    // ==========================================
    // TESTS PARA CALLE
    // ==========================================

    @Test
    public void testSetCalleValida() {
        asociado.setCalle("Av. Colón");
        assertEquals("Av. Colón", asociado.getCalle(), "La calle asignada debe recuperarse correctamente.");
    }

    @Test
    public void testSetCalleNulaLanzaError() {
        assertThrows(AssertionError.class, () -> asociado.setCalle(null), 
                "Debe fallar con AssertionError al asignar una calle nula.");
    }

    // ==========================================
    // TESTS PARA TELEFONO
    // ==========================================

    @Test
    public void testSetTelefonoValido() {
        asociado.setTelefono("2231234567");
        assertEquals("2231234567", asociado.getTelefono(), "El teléfono asignado debe recuperarse correctamente.");
    }

    @Test
    public void testSetTelefonoNuloLanzaError() {
        assertThrows(AssertionError.class, () -> asociado.setTelefono(null), 
                "Debe fallar con AssertionError al asignar un teléfono nulo.");
    }

    // ==========================================
    // TESTS PARA NUMERO (VALORES LÍMITE)
    // ==========================================

    @Test
    public void testSetNumeroValidoLimiteInferior() {
        // Valor límite inferior según SRS (numero >= 0). 
        // Con el código provisto (assert numero > 0), este test detecta el bug y falla en rojo.
        asociado.setNumero(0);
        assertEquals(0, asociado.getNumero(), "El sistema debe aceptar el 0 como valor límite válido.");
    }

    @Test
    public void testSetNumeroValido() {
        asociado.setNumero(1);
        assertEquals(1, asociado.getNumero(), "Debe permitir un número entero positivo.");
    }

    @Test
    public void testSetNumeroInvalidoLimite() {
        assertThrows(AssertionError.class, () -> asociado.setNumero(-1), 
                "Se esperaba AssertionError al ingresar un número negativo.");
    }
}