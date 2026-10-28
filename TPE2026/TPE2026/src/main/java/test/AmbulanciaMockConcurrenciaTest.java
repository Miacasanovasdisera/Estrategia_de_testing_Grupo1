package test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;
import modelo.modeloDominio.ambulancia.State;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;

public class AmbulanciaMockConcurrenciaTest {

    private Ambulancia ambulancia;
    private State estadoMock;

    @BeforeEach
    public void setUp() {
        ambulancia = new Ambulancia();
        // Doble de prueba (Mock) para desacoplar de los estados concretos
        estadoMock = mock(State.class);
        ambulancia.setState(estadoMock);
    }

    // =========================================================================
    // 1. PRUEBAS ATÓMICAS CON MOCKS (LAS 5 PASAN EN VERDE)
    // =========================================================================

    @Test
    @DisplayName("CP-MOCK-01: Delegación de Solicitud de Mantenimiento al Estado")
    public void testDelegacionSolicitarMantenimiento() {
        Operario operario = new Operario(null, null, null, null, null, ambulancia);
        ambulancia.solicitarMantenimiento(operario);

        verify(estadoMock, times(1)).SolicitudMantenimiento();
    }

    @Test
    @DisplayName("CP-MOCK-02: Delegación de Atención a Domicilio al Estado")
    public void testDelegacionAtenderDomicilio() {
        Asociado asociado = mock(Asociado.class);
        ambulancia.atenderDomicilio(asociado);

        verify(estadoMock, times(1)).SolicitudDeAtencionDomicilio();
    }

    @Test
    @DisplayName("CP-MOCK-03: Delegación de Traslado a la Clínica al Estado")
    public void testDelegacionTrasladarALaClinica() {
        Asociado asociado = mock(Asociado.class);
        ambulancia.trasladarALaClinica(asociado);

        verify(estadoMock, times(1)).SolicitudDeTraslado();
    }

    @Test
    @DisplayName("CP-MOCK-04: Delegación de Regreso Sin Paciente al Estado")
    public void testDelegacionRegresarSinPaciente() {
        ambulancia.regresarSinPaciente();

        verify(estadoMock, times(1)).RetornoClinica();
    }

    @Test
    @DisplayName("CP-MOCK-05: Propagación de Excepciones del Estado Simulado")
    public void testPropagacionFalloEnEstado() {
        doThrow(new IllegalStateException("Transición inválida")).when(estadoMock).SolicitudMantenimiento();

        assertThrows(IllegalStateException.class, () -> {
            ambulancia.solicitarMantenimiento(new Operario(null, null, null, null, null, ambulancia));
        });
    }

    // =========================================================================
    // 2. PRUEBAS DE INTEGRACIÓN Y CONCURRENCIA (LOS 3 FAILURES EN ROJO)
    // =========================================================================

    @Test
    @DisplayName("CP-CONC-01: Detección de Falla en Ciclo de RetornoAutomatico (Simulación Activa)")
    public void testCicloRetornoAutomaticoConSimulacionActiva() throws InterruptedException {
        // Configuramos la simulación en activa
        ambulancia.setSimulacionActiva(true);

        // Usamos la clase real provista por la cátedra
        RetornoAutomatico rutina = new RetornoAutomatico(ambulancia);
        Thread hilo = new Thread(rutina);
        hilo.start();

        // Pausa breve para permitir la inicialización del hilo
        Thread.sleep(100);

        // ¡FALLO 1 (FAILURE)!
        // El hilo debería mantenerse en ejecución ejecutando su ciclo mientras la simulación esté activa.
        // Como la condición del while está negada (!isSimulacionActiva), el hilo finalizó de inmediato.
        assertTrue(hilo.isAlive(), "FALLO DETECTADO: El hilo de RetornoAutomatico murió de inmediato porque while (!isSimulacionActiva()) está invertido.");
    }

    @Test
    @DisplayName("CP-CONC-02: Detección de Fallo de Estado en Hilo RetornoAutomático")
    public void testRetornoAutomaticoInvocaMetodoClinica() throws InterruptedException {
        // Configuramos la simulación activa
        ambulancia.setSimulacionActiva(true);

        // Lanzamos la rutina en su propio hilo
        RetornoAutomatico rutina = new RetornoAutomatico(ambulancia);
        Thread hiloRetorno = new Thread(rutina);
        hiloRetorno.start();

        // Esperamos a que el hilo termine su ejecución prematura
        hiloRetorno.join(300);

        // ¡FALLO 2 (FAILURE)!
        // Mockito arroja WantedButNotInvoked: estadoMock.RetornoClinica()
        // porque el hilo murió antes de llegar a ambulancia.retornoAutomatico(this).
        verify(estadoMock, atLeastOnce()).RetornoClinica();
    }

    @Test
    @DisplayName("CP-CONC-03: Sincronización de Recursos Compartidos ante Múltiples Hilos")
    public void testAccesoConcurrenteAmbulancia() throws InterruptedException {
        final int CANTIDAD_HILOS = 10;
        final int ITERACIONES = 100;
        CountDownLatch latch = new CountDownLatch(CANTIDAD_HILOS);
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());

        Runnable tareaConcurrente = () -> {
            try {
                for (int i = 0; i < ITERACIONES; i++) {
                    ambulancia.setOcupado(i % 2 == 0);
                    boolean ocupado = ambulancia.isOcupado();
                    if (ocupado != (i % 2 == 0)) {
                        throw new IllegalStateException("Condición de carrera detectada en ambulancia.isOcupado()");
                    }
                }
            } catch (Throwable t) {
                errores.add(t);
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < CANTIDAD_HILOS; i++) {
            new Thread(tareaConcurrente).start();
        }

        boolean finalizaron = latch.await(5, TimeUnit.SECONDS);

        assertTrue(finalizaron, "Los hilos concurrentes quedaron bloqueados (Deadlock o Timeout).");
        
        // ¡FALLO 3 (FAILURE)!
        // Falla porque la lista no está vacía: se capturan las excepciones de la condición de carrera.
        assertTrue(errores.isEmpty(), "Se detectaron problemas de concurrencia/sincronización en la ambulancia: " + errores);
    }
}