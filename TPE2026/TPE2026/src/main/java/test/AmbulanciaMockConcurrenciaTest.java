package test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        // Mock de State para no depender de los estados concretos
        estadoMock = mock(State.class);
        ambulancia.setState(estadoMock);
    }

    @Test
    @DisplayName("CP-MOCK-01: Delegación de Solicitud de Mantenimiento al Estado")
    public void testDelegacionSolicitarMantenimiento() {
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

    @Test
    @DisplayName("CP-CONC-01: Detección de Fallo de Estado en Hilo RetornoAutomático")
    public void testRetornoAutomaticoInvocaMetodoClinica() throws InterruptedException {
        ambulancia.setSimulacionActiva(true);

        RetornoAutomatico rutina = new RetornoAutomatico(ambulancia);
        Thread hiloRetorno = new Thread(rutina);
        hiloRetorno.start();

        // RetornoAutomatico espera 10 s antes de cada retorno
        try {
            verify(estadoMock, timeout(11000).atLeastOnce()).RetornoClinica();
        } finally {
            ambulancia.setSimulacionActiva(false);
            hiloRetorno.interrupt();
        }
    }

    @Test
    @DisplayName("CP-CONC-02: El retorno automático debe liberar a los hilos en espera")
    public void testRetornoAutomaticoLiberaHilosEnEspera() throws InterruptedException {
        ambulancia.setOcupado(true);
        Thread solicitante = new Thread(() -> ambulancia.atenderDomicilio(mock(Asociado.class)));
        solicitante.start();

        // Esperar a que el hilo quede en wait() con la ambulancia ocupada
        long limite = System.currentTimeMillis() + 1000;
        while (solicitante.getState() != Thread.State.WAITING && System.currentTimeMillis() < limite) {
            Thread.sleep(10);
        }
        assertEquals(Thread.State.WAITING, solicitante.getState(), "El hilo debería estar esperando la ambulancia.");

        ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));

        try {
            verify(estadoMock, timeout(1000)).SolicitudDeAtencionDomicilio();
        } finally {
            solicitante.interrupt();
        }
    }
}