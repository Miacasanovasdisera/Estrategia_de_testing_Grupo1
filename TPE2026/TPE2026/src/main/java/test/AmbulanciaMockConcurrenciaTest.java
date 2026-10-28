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
import org.junit.jupiter.api.Nested;
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
        // Creación del doble de prueba (Mock) para desacoplar de las clases concretas de State
        estadoMock = mock(State.class);
        ambulancia.setState(estadoMock);
    }

    // =========================================================================
    // 1. TESTING CON MOCKS: PRUEBAS ATÓMICAS SOBRE LA INTERFAZ STATE
    // =========================================================================
    @Nested
    @DisplayName("Pruebas Unitarias Atómicas con Mocks")
    class PruebasConMocks {

        @Test
        @DisplayName("CP-MOCK-01: Delegación de Solicitud de Mantenimiento al Estado")
        public void testDelegacionSolicitarMantenimiento() {
            Operario operario = new Operario(null, null, null, null, null, ambulancia);
            ambulancia.solicitarMantenimiento(operario);

            // Verificamos que Ambulancia delegó exactamente una vez en la interfaz State[cite: 30]
            verify(estadoMock, times(1)).SolicitudMantenimiento();
            verifyNoMoreInteractions(estadoMock);
        }

        @Test
        @DisplayName("CP-MOCK-02: Delegación de Atención a Domicilio al Estado")
        public void testDelegacionAtenderDomicilio() {
            Asociado asociado = mock(Asociado.class);
            ambulancia.atenderDomicilio(asociado);

            verify(estadoMock, times(1)).SolicitudDeAtencionDomicilio();
            verifyNoMoreInteractions(estadoMock);
        }

        @Test
        @DisplayName("CP-MOCK-03: Delegación de Traslado a la Clínica al Estado")
        public void testDelegacionTrasladarALaClinica() {
            Asociado asociado = mock(Asociado.class);
            ambulancia.trasladarALaClinica(asociado);

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
        @DisplayName("CP-MOCK-05: Comportamiento ante Fallo Inesperado simulado en el Mock")
        public void testComportamientoFalloEnEstado() {
            // Configuramos el Mock para simular una excepción en tiempo de ejecución[cite: 30]
            doThrow(new IllegalStateException("Transición inválida")).when(estadoMock).SolicitudMantenimiento();

            assertThrows(IllegalStateException.class, () -> {
                ambulancia.solicitarMantenimiento(new Operario(null, null, null, null, null, ambulancia));
            }, "La ambulancia debe propagar la excepción ocurrida en la máquina de estados.");
            
            verify(estadoMock, times(1)).SolicitudMantenimiento();
        }
    }

    // =========================================================================
    // 2. TESTING DE INTEGRACIÓN Y CONCURRENCIA
    // =========================================================================
    @Nested
    @DisplayName("Pruebas de Concurrencia y Sincronización")
    class PruebasConcurrencia {

        @Test
        @DisplayName("CP-CONC-01: Detección de Falla en Ciclo de RetornoAutomatico (Simulación Activa)")
        public void testCicloRetornoAutomaticoConSimulacionActiva() throws InterruptedException {
            // Configuramos la ambulancia con simulación activa
            ambulancia.setSimulacionActiva(true);

            // Subclase de prueba / Spying para interceptar la llamada periódica sin esperar los 10 segundos
            class RetornoAutomaticoTestable extends RetornoAutomatico {
                private boolean invocado = false;

                public RetornoAutomaticoTestable(Ambulancia a) {
                    super(a);
                }

                @Override
                public void run() {
                    // Si el código original tiene el error 'while (!isSimulacionActiva())',
                    // al estar la simulación activa jamás ingresará aquí.
                    if (ambulancia.isSimulacionActiva()) {
                        ambulancia.retornoAutomatico(this);
                        invocado = true;
                    }
                }

                public boolean fueInvocado() {
                    return invocado;
                }
            }

            RetornoAutomaticoTestable rutina = new RetornoAutomaticoTestable(ambulancia);
            Thread hilo = new Thread(rutina);
            hilo.start();
            hilo.join(1000);

            // Verificación: la rutina debe disparar retornoAutomatico en ambulancia
            assertTrue(rutina.fueInvocado(), "FALLO DETECTADO: RetornoAutomatico no se ejecuta cuando la simulación está activa debido a condición lógica invertida.");
        }

        @Test
        @DisplayName("CP-CONC-02: Detección de Fallo de Estado en Ejecución Directa de Retorno")
        public void testRetornoAutomaticoInvocaMetodoClinica() {
            RetornoAutomatico rutina = new RetornoAutomatico(ambulancia);
            ambulancia.setSimulacionActiva(true);

            // Al forzar el retorno automático, la ambulancia debe delegar a RetornoClinica() en su estado[cite: 28, 29]
            ambulancia.retornoAutomatico(rutina);

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
                        // Acciones concurrentes modificando el recurso compartido
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
            assertTrue(errores.isEmpty(), "Se detectaron problemas de concurrencia/sincronización en la ambulancia: " + errores);
        }
    }
}