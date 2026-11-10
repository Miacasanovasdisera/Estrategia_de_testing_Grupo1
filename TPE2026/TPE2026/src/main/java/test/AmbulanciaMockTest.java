package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;
import test.dobles.ObservadorEspia;
import test.dobles.StateMock;

/**
 * Pruebas atomicas de Ambulancia usando un doble de prueba (StateMock) en lugar
 * de los estados concretos. Verifican que Ambulancia delega correctamente en su
 * estado, gestiona el flag "ocupado" y respeta la exclusion mutua, independientemente
 * de la logica de cada estado.
 *
 * IMPORTANTE: ejecutar con la opcion de VM -ea (assertions habilitadas).
 */
public class AmbulanciaMockTest {

    private Ambulancia ambulancia;
    private StateMock estadoMock;
    private ObservadorEspia espia;
    private Asociado asociado;
    private Operario operario;

    @BeforeEach
    public void setUp() {
        ambulancia = new Ambulancia();
        estadoMock = new StateMock();
        ambulancia.setState(estadoMock);
        espia = new ObservadorEspia();
        ambulancia.addObserver(espia);

        Domicilio dom = new Domicilio("Alem", 100, "Mar del Plata");
        asociado = new Asociado("Juan", "State", "12345678", "2235551234", dom);
        operario = new Operario("Ana", "Taller", "87654321", "2235555678", dom, ambulancia);
    }

    // =========================================================================
    // 1. setState / getEstadoActual
    // =========================================================================
    @Test
    public void testSetStateAsignaElDoble() {
        assertSame(estadoMock, ambulancia.getEstadoActual(), "La ambulancia debe usar el estado inyectado.");
    }

    @Test
    public void testSetStateNuloLanzaError() {
        // Clase de equivalencia invalida: estado nulo
        assertThrows(AssertionError.class, () -> ambulancia.setState(null),
                "setState(null) debe ser rechazado por la precondicion.");
    }

    // =========================================================================
    // 2. Delegacion de cada solicitud en el estado (ambulancia libre)
    // =========================================================================
    @Test
    public void testTrasladarDelegaEnSolicitudDeTraslado() {
        ambulancia.trasladarALaClinica(asociado);

        assertEquals(Arrays.asList(StateMock.TRASLADO), estadoMock.llamadas(),
                "Debe delegar exactamente una vez en SolicitudDeTraslado y en nada mas.");
        assertTrue(ambulancia.isOcupado(), "La ambulancia debe quedar ocupada.");
        assertTrue(espia.recibioTipo("OK"), "Debe notificar a los observadores el traslado.");
    }

    @Test
    public void testAtenderDomicilioDelegaEnSolicitudDeAtencionDomicilio() {
        ambulancia.atenderDomicilio(asociado);

        assertEquals(Arrays.asList(StateMock.DOMICILIO), estadoMock.llamadas());
        assertTrue(ambulancia.isOcupado());
        assertTrue(espia.recibioTipo("OK"));
    }

    @Test
    public void testSolicitarMantenimientoDelegaEnSolicitudMantenimiento() {
        ambulancia.solicitarMantenimiento(operario);

        assertEquals(Arrays.asList(StateMock.MANTENIMIENTO), estadoMock.llamadas());
        assertTrue(ambulancia.isOcupado());
        assertTrue(espia.recibioTipo("OK"));
    }

    @Test
    public void testVolviendoDelTallerDelegaYLiberaAmbulancia() {
        ambulancia.setOcupado(true);

        ambulancia.volviendoDelTaller();

        assertEquals(Arrays.asList(StateMock.MANTENIMIENTO), estadoMock.llamadas());
        assertFalse(ambulancia.isOcupado(), "Al volver del taller la ambulancia debe liberarse.");
    }

    @Test
    public void testRegresarSinPacienteDelegaYLiberaAmbulancia() {
        ambulancia.setOcupado(true);

        ambulancia.regresarSinPaciente();

        assertEquals(Arrays.asList(StateMock.RETORNO), estadoMock.llamadas());
        assertFalse(ambulancia.isOcupado(), "Al regresar la ambulancia debe liberarse.");
    }

    @Test
    public void testRetornoAutomaticoDelegaEnRetornoClinica() {
        ambulancia.retornoAutomatico(null);

        assertEquals(Arrays.asList(StateMock.RETORNO), estadoMock.llamadas());
    }

    // =========================================================================
    // 3. Precondiciones (clases invalidas)
    // =========================================================================
    @Test
    public void testTrasladarAsociadoNuloLanzaError() {
        assertThrows(AssertionError.class, () -> ambulancia.trasladarALaClinica(null));
        assertEquals(0, estadoMock.totalLlamadas(), "No debe tocar el estado si la precondicion falla.");
    }

    @Test
    public void testAtenderDomicilioAsociadoNuloLanzaError() {
        assertThrows(AssertionError.class, () -> ambulancia.atenderDomicilio(null));
        assertEquals(0, estadoMock.totalLlamadas());
    }

    @Test
    public void testSolicitarMantenimientoOperarioNuloLanzaError() {
        assertThrows(AssertionError.class, () -> ambulancia.solicitarMantenimiento(null));
        assertEquals(0, estadoMock.totalLlamadas());
    }

    // =========================================================================
    // 4. Simulacion inactiva: no se delega nada
    // =========================================================================
    @Test
    public void testSimulacionInactivaNoDelegaSolicitudes() {
        ambulancia.setSimulacionActiva(false);

        ambulancia.trasladarALaClinica(asociado);
        ambulancia.atenderDomicilio(asociado);
        ambulancia.solicitarMantenimiento(operario);

        assertEquals(0, estadoMock.totalLlamadas(), "Con la simulacion terminada no debe delegar en el estado.");
        assertFalse(ambulancia.isOcupado(), "Con la simulacion terminada no debe ocuparse.");
    }

    // =========================================================================
    // 5. Exclusion mutua y wait/notify, observados a traves del mock
    // =========================================================================
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testSolicitudConAmbulanciaOcupadaEsperaHastaLiberacion() throws Exception {
        ambulancia.setOcupado(true);

        Thread hilo = new Thread(() -> ambulancia.trasladarALaClinica(asociado));
        hilo.start();
        Concurrencia.esperarEstado(hilo, Thread.State.WAITING);

        // Mientras espera: no debe haber tocado el estado y debe haber avisado
        assertEquals(0, estadoMock.veces(StateMock.TRASLADO),
                "No debe delegar el traslado mientras la ambulancia esta ocupada.");
        assertTrue(espia.recibioTipo("NO_AMBULANCIA"), "Debe notificar que la ambulancia esta ocupada.");

        // Liberacion: regresarSinPaciente hace notifyAll
        ambulancia.regresarSinPaciente();
        hilo.join(2000);

        assertFalse(hilo.isAlive(), "El hilo en espera debe despertarse tras la liberacion.");
        assertEquals(Arrays.asList(StateMock.RETORNO, StateMock.TRASLADO), estadoMock.llamadas(),
                "Primero el retorno y recien despues el traslado pendiente.");
        assertTrue(ambulancia.isOcupado(), "El hilo despertado debe haber tomado la ambulancia.");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testFinalizarSimulacionDespiertaHilosSinDelegar() throws Exception {
        ambulancia.setOcupado(true);

        Thread h1 = new Thread(() -> ambulancia.atenderDomicilio(asociado));
        Thread h2 = new Thread(() -> ambulancia.solicitarMantenimiento(operario));
        h1.start();
        h2.start();
        Concurrencia.esperarEstado(h1, Thread.State.WAITING);
        Concurrencia.esperarEstado(h2, Thread.State.WAITING);

        ambulancia.setSimulacionActiva(false);
        h1.join(2000);
        h2.join(2000);

        assertFalse(h1.isAlive(), "setSimulacionActiva(false) debe despertar a los hilos en espera.");
        assertFalse(h2.isAlive(), "setSimulacionActiva(false) debe despertar a los hilos en espera.");
        assertEquals(0, estadoMock.totalLlamadas(), "Los hilos despertados no deben delegar en el estado.");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testInterrupcionDuranteEsperaNoDelega() throws Exception {
        ambulancia.setOcupado(true);
        final boolean[] flagInterrupcion = new boolean[1];

        Thread hilo = new Thread(() -> {
            ambulancia.trasladarALaClinica(asociado);
            flagInterrupcion[0] = Thread.currentThread().isInterrupted();
        });
        hilo.start();
        Concurrencia.esperarEstado(hilo, Thread.State.WAITING);

        hilo.interrupt();
        hilo.join(2000);

        assertFalse(hilo.isAlive(), "El hilo interrumpido debe terminar.");
        assertTrue(flagInterrupcion[0], "Debe preservarse el flag de interrupcion.");
        assertEquals(0, estadoMock.totalLlamadas(), "Un hilo interrumpido no debe delegar en el estado.");
    }

    // =========================================================================
    // 6. Mock con comportamiento programado: simula una transicion de estado
    // =========================================================================
    @Test
    public void testMockProgramadoSimulaTransicion() {
        StateMock siguiente = new StateMock();
        estadoMock.programar(StateMock.TRASLADO, () -> ambulancia.setState(siguiente));

        ambulancia.trasladarALaClinica(asociado);
        ambulancia.regresarSinPaciente();

        assertSame(siguiente, ambulancia.getEstadoActual(), "La transicion simulada por el mock debe aplicarse.");
        assertEquals(1, estadoMock.veces(StateMock.TRASLADO));
        assertEquals(1, siguiente.veces(StateMock.RETORNO),
                "La siguiente solicitud debe llegar al nuevo estado, no al anterior.");
        assertEquals(0, estadoMock.veces(StateMock.RETORNO));
    }
}
