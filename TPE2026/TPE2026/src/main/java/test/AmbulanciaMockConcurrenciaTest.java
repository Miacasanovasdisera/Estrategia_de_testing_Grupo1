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
        // Doble de prueba (Mock) para desacoplar de las implementaciones concretas de State
        estadoMock = mock(State.class);
        ambulancia.setState(estadoMock);
    }

    // =========================================================================
    // 1. PRUEBAS ATÓMICAS CON MOCKS (PASAN EN VERDE)
    // =========================================================================

    @Test
    @DisplayName("CP-MOCK-01: Delegación de Solicitud de Mantenimiento al Estado")
    public void testDelegacionSolicitarMantenimiento() {
        // Se utiliza un mock para Operario evitando constructores con precondiciones nulas
        Operario operarioMock = mock(Operario.class);
        ambulancia.solicitarMantenimiento(operarioMock);

        verify(estadoMock, times(1)).SolicitudMantenimiento();
        verifyNoMoreInteractions(estadoMock);
    }

    @Test
    @DisplayName("CP-MOCK-02: Delegación de Atención a Domicilio al Estado")
    public void testDelegacionAtenderDomicilio() {
        Asociado asociadoMock = mock(Asociado.class);
        when(asociadoMock.getNombre()).thenReturn("Juan");

        ambulancia.atenderDomicilio(asociadoMock);

        verify(estadoMock, times(1)).SolicitudDeAtencionDomicilio();
        verifyNoMoreInteractions(estadoMock);
    }

    @Test
    @DisplayName("CP-MOCK-03: Delegación de Traslado a la Clínica al Estado")
    public void testDelegacionTrasladarALaClinica() {
        Asociado asociadoMock = mock(Asociado.class);
        ambulancia.trasladarALaClinica(asociadoMock);

        verify(estadoMock, times(1)).SolicitudDeTraslado();
        verifyNoMoreInteractions(estadoMock);
    }

    @Test
    @DisplayName("CP-MOCK-04: Delegación de Regreso Sin Paciente al Estado")
    public void testDelegacionRegresarSinPaciente() {
        ambulancia.regresarSinPaciente();

        verify(estadoMock, times(1)).RetornoClinica();
        verifyNoMoreInteractions(estadoMock);
    }

    @Test
    @DisplayName("CP-MOCK-05: Propagación de Excepciones del Estado Simulado")
    public void testPropagacionFalloEnEstado() {
        Operario operarioMock = mock(Operario.class);
        doThrow(new IllegalStateException("Transición de estado inválida")).when(estadoMock).SolicitudMantenimiento();

        assertThrows(IllegalStateException.class, () -> {
            ambulancia.solicitarMantenimiento(operarioMock);
        }, "La ambulancia debe propagar la excepción lanzada por la máquina de estados.");
    }

    // =========================================================================
    // 2. PRUEBAS DE INTEGRACIÓN Y CONCURRENCIA (LOS 3 FAILURES EN ROJO)
    // =========================================================================

    @Test
    @DisplayName("CP-CONC-01: Detección de Falla en Ciclo de RetornoAutomatico (Simulación Activa)")
    public void testCicloRetornoAutomaticoConSimulacionActiva() throws InterruptedException {
        ambulancia.setSimulacionActiva(true);

        RetornoAutomatico rutina = new RetornoAutomatico(ambulancia);
        Thread hilo = new Thread(rutina);
        hilo.start();

        // Pausa breve para permitir la inicialización del ciclo en el hilo
        Thread.sleep(100);

        // ¡FALLO 1!: El bucle while (!isSimulacionActiva()) con simulación en true
        // aborta instantáneamente, provocando la terminación del hilo.
        assertTrue(hilo.isAlive(), "FALLO: El hilo de RetornoAutomatico finalizó prematuramente debido a la condición invertida en el bucle while.");
    }

    @Test
    @DisplayName("CP-CONC-02: Detección de Fallo de Estado en Hilo RetornoAutomático")
    public void testRetornoAutomaticoInvocaMetodoClinica() throws InterruptedException {
        ambulancia.setSimulacionActiva(true);

        RetornoAutomatico rutina = new RetornoAutomatico(ambulancia);
        Thread hiloRetorno = new Thread(rutina);
        hiloRetorno.start();

        hiloRetorno.join(300);

        // ¡FALLO 2!: Mockito arroja WantedButNotInvoked sobre RetornoClinica()
        // porque el hilo terminó antes de alcanzar la línea ambulancia.retornoAutomatico(this).
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
        
        // ¡FALLO 3!: Al carecer de 'synchronized', se producen colisiones de lectura/escritura
        // y la lista de errores contiene excepciones de condición de carrera.
        assertTrue(errores.isEmpty(), "Se detectaron problemas de concurrencia/sincronización en la ambulancia: " + errores);
    }
}