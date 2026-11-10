package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.AtendiendoEnDomicilioState;
import modelo.modeloDominio.ambulancia.DisponibleState;
import modelo.modeloDominio.ambulancia.EnTallerState;
import modelo.modeloDominio.ambulancia.RegresandoClinicaSinPacienteState;
import modelo.modeloDominio.ambulancia.RegresandoDelTallerState;
import modelo.modeloDominio.ambulancia.State;
import modelo.modeloDominio.ambulancia.TrasladandoPacienteClinicaState;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

/**
 * Pruebas de integracion de la maquina de estados real acoplada a Ambulancia,
 * y pruebas de concurrencia con hilos reales sobre el recurso compartido.
 * El oraculo de transiciones es el diagrama UML-State2.png del proyecto.
 *
 * IMPORTANTE: ejecutar con la opcion de VM -ea (assertions habilitadas).
 */
public class AmbulanciaIntegracionTest {

    private Ambulancia ambulancia;
    private Asociado asociado;
    private Operario operario;

    @BeforeEach
    public void setUp() {
        ambulancia = new Ambulancia();
        Domicilio dom = new Domicilio("Alem", 100, "Mar del Plata");
        asociado = new Asociado("Juan", "State", "12345678", "2235551234", dom);
        operario = new Operario("Ana", "Taller", "87654321", "2235555678", dom, ambulancia);
    }

    // =========================================================================
    // 1. Estado inicial
    // =========================================================================
    @Test
    public void testEstadoInicial() {
        assertInstanceOf(DisponibleState.class, ambulancia.getEstadoActual(), "Una ambulancia nueva debe estar Disponible.");
        assertFalse(ambulancia.isOcupado(), "Una ambulancia nueva no debe estar ocupada.");
        assertTrue(ambulancia.isSimulacionActiva(), "La simulacion debe iniciar activa.");
    }

    // =========================================================================
    // 2. Tabla de transiciones: 6 estados x 4 eventos = 24 casos
    //    (cada estado es una clase de equivalencia; cada evento se prueba en todas)
    // =========================================================================
    private static final Consumer<State> TRASLADO = State::SolicitudDeTraslado;
    private static final Consumer<State> DOMICILIO = State::SolicitudDeAtencionDomicilio;
    private static final Consumer<State> MANTENIMIENTO = State::SolicitudMantenimiento;
    private static final Consumer<State> RETORNO = State::RetornoClinica;

    private static Arguments caso(String nombre, Function<Ambulancia, State> inicial,
                                  Consumer<State> evento, Class<? extends State> esperado) {
        return Arguments.of(nombre, inicial, evento, esperado);
    }

    static Stream<Arguments> tablaDeTransiciones() {
        return Stream.of(
                // Disponible
                caso("Disponible + traslado -> Trasladando", DisponibleState::new, TRASLADO, TrasladandoPacienteClinicaState.class),
                caso("Disponible + domicilio -> Atendiendo", DisponibleState::new, DOMICILIO, AtendiendoEnDomicilioState.class),
                caso("Disponible + mantenimiento -> EnTaller", DisponibleState::new, MANTENIMIENTO, EnTallerState.class),
                caso("Disponible + retorno -> Disponible", DisponibleState::new, RETORNO, DisponibleState.class),
                // Trasladando paciente a la clinica
                caso("Trasladando + traslado -> Trasladando", TrasladandoPacienteClinicaState::new, TRASLADO, TrasladandoPacienteClinicaState.class),
                caso("Trasladando + domicilio -> Trasladando", TrasladandoPacienteClinicaState::new, DOMICILIO, TrasladandoPacienteClinicaState.class),
                caso("Trasladando + mantenimiento -> Trasladando", TrasladandoPacienteClinicaState::new, MANTENIMIENTO, TrasladandoPacienteClinicaState.class),
                caso("Trasladando + retorno -> Disponible", TrasladandoPacienteClinicaState::new, RETORNO, DisponibleState.class),
                // Atendiendo en domicilio
                caso("Atendiendo + traslado -> Atendiendo", AtendiendoEnDomicilioState::new, TRASLADO, AtendiendoEnDomicilioState.class),
                caso("Atendiendo + domicilio -> Atendiendo", AtendiendoEnDomicilioState::new, DOMICILIO, AtendiendoEnDomicilioState.class),
                caso("Atendiendo + mantenimiento -> Atendiendo", AtendiendoEnDomicilioState::new, MANTENIMIENTO, AtendiendoEnDomicilioState.class),
                caso("Atendiendo + retorno -> RegresandoSinPaciente", AtendiendoEnDomicilioState::new, RETORNO, RegresandoClinicaSinPacienteState.class),
                // Regresando a la clinica sin paciente
                caso("RegresandoSinPaciente + traslado -> Trasladando", RegresandoClinicaSinPacienteState::new, TRASLADO, TrasladandoPacienteClinicaState.class),
                caso("RegresandoSinPaciente + domicilio -> Atendiendo", RegresandoClinicaSinPacienteState::new, DOMICILIO, AtendiendoEnDomicilioState.class),
                caso("RegresandoSinPaciente + mantenimiento -> RegresandoSinPaciente", RegresandoClinicaSinPacienteState::new, MANTENIMIENTO, RegresandoClinicaSinPacienteState.class),
                caso("RegresandoSinPaciente + retorno -> Disponible", RegresandoClinicaSinPacienteState::new, RETORNO, DisponibleState.class),
                // En taller
                caso("EnTaller + traslado -> EnTaller", EnTallerState::new, TRASLADO, EnTallerState.class),
                caso("EnTaller + domicilio -> EnTaller", EnTallerState::new, DOMICILIO, EnTallerState.class),
                caso("EnTaller + mantenimiento -> RegresandoDelTaller", EnTallerState::new, MANTENIMIENTO, RegresandoDelTallerState.class),
                caso("EnTaller + retorno -> EnTaller", EnTallerState::new, RETORNO, EnTallerState.class),
                // Regresando del taller
                caso("RegresandoDelTaller + traslado -> RegresandoDelTaller", RegresandoDelTallerState::new, TRASLADO, RegresandoDelTallerState.class),
                caso("RegresandoDelTaller + domicilio -> RegresandoDelTaller", RegresandoDelTallerState::new, DOMICILIO, RegresandoDelTallerState.class),
                caso("RegresandoDelTaller + mantenimiento -> RegresandoDelTaller", RegresandoDelTallerState::new, MANTENIMIENTO, RegresandoDelTallerState.class),
                caso("RegresandoDelTaller + retorno -> Disponible", RegresandoDelTallerState::new, RETORNO, DisponibleState.class)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablaDeTransiciones")
    public void testTransicion(String nombre, Function<Ambulancia, State> inicial,
                               Consumer<State> evento, Class<? extends State> esperado) {
        State estadoInicial = inicial.apply(ambulancia);
        ambulancia.setState(estadoInicial);

        evento.accept(estadoInicial);

        assertInstanceOf(esperado, ambulancia.getEstadoActual(), "Transicion incorrecta: " + nombre);
    }

    // =========================================================================
    // 3. Flujos completos a traves de la API publica de Ambulancia
    // =========================================================================
    @Test
    public void testCicloCompletoAtencionDomicilio() {
        ambulancia.atenderDomicilio(asociado);
        assertInstanceOf(AtendiendoEnDomicilioState.class, ambulancia.getEstadoActual());
        assertTrue(ambulancia.isOcupado());

        ambulancia.regresarSinPaciente();
        assertInstanceOf(RegresandoClinicaSinPacienteState.class, ambulancia.getEstadoActual());
        assertFalse(ambulancia.isOcupado());

        ambulancia.retornoAutomatico(null);
        assertInstanceOf(DisponibleState.class, ambulancia.getEstadoActual());
    }

    @Test
    public void testCicloCompletoTraslado() {
        ambulancia.trasladarALaClinica(asociado);
        assertInstanceOf(TrasladandoPacienteClinicaState.class, ambulancia.getEstadoActual());
        assertTrue(ambulancia.isOcupado());

        ambulancia.regresarSinPaciente();
        assertInstanceOf(DisponibleState.class, ambulancia.getEstadoActual(),
                "Tras dejar al paciente en la clinica la ambulancia debe quedar Disponible (UML).");
        assertFalse(ambulancia.isOcupado());
    }

    @Test
    public void testCicloCompletoMantenimiento() {
        ambulancia.solicitarMantenimiento(operario);
        assertInstanceOf(EnTallerState.class, ambulancia.getEstadoActual());
        assertTrue(ambulancia.isOcupado());

        ambulancia.volviendoDelTaller();
        assertInstanceOf(RegresandoDelTallerState.class, ambulancia.getEstadoActual());
        assertFalse(ambulancia.isOcupado());

        ambulancia.retornoAutomatico(null);
        assertInstanceOf(DisponibleState.class, ambulancia.getEstadoActual());
    }

    @Test
    public void testDosTrasladosConsecutivosNoPierdenLaSolicitud() {
        ambulancia.trasladarALaClinica(asociado);
        ambulancia.regresarSinPaciente();

        ambulancia.trasladarALaClinica(asociado);

        assertInstanceOf(TrasladandoPacienteClinicaState.class, ambulancia.getEstadoActual(),
                "El segundo traslado fue aceptado por la ambulancia pero el estado no lo refleja (solicitud perdida).");
    }

    // =========================================================================
    // 4. Concurrencia con hilos reales
    // =========================================================================
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testRegresarSinPacienteNoRetieneElMonitor() throws Exception {
        ambulancia.trasladarALaClinica(asociado);

        Thread regreso = new Thread(() -> ambulancia.regresarSinPaciente());
        regreso.start();
        // Si el estado duerme dentro del bloque synchronized, el hilo queda TIMED_WAITING con el monitor tomado
        long limite = System.currentTimeMillis() + 200;
        while (regreso.isAlive() && regreso.getState() != Thread.State.TIMED_WAITING
                && System.currentTimeMillis() < limite) {
            Thread.sleep(1);
        }

        long inicio = System.nanoTime();
        ambulancia.isOcupado();   // metodo synchronized: necesita el monitor de la ambulancia
        long bloqueadoMs = (System.nanoTime() - inicio) / 1_000_000;
        regreso.join(3000);

        assertTrue(bloqueadoMs < 200,
                "Otro hilo quedo bloqueado " + bloqueadoMs + " ms esperando el monitor: el estado retiene el lock (sleep dentro de synchronized).");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testRegresarSinPacienteConHiloInterrumpidoNoPierdeTransicionNiFlag() throws Exception {
        ambulancia.trasladarALaClinica(asociado);
        final boolean[] flag = new boolean[1];

        Thread hilo = new Thread(() -> {
            Thread.currentThread().interrupt();   // el hilo llega interrumpido
            ambulancia.regresarSinPaciente();
            flag[0] = Thread.currentThread().isInterrupted();
        });
        hilo.start();
        hilo.join(3000);

        assertFalse(ambulancia.getEstadoActual() instanceof TrasladandoPacienteClinicaState,
                "Una interrupcion no debe impedir la transicion: la ambulancia quedo trabada en Trasladando.");
        assertTrue(flag[0], "El flag de interrupcion no debe ser tragado por el estado.");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testExclusionMutuaYVivacidadConMultiplesAsociados() throws Exception {
        final int hilos = 4;
        final int solicitudesPorHilo = 3;
        final AtomicInteger dentro = new AtomicInteger(0);
        final AtomicInteger maxSimultaneos = new AtomicInteger(0);
        final AtomicInteger atendidas = new AtomicInteger(0);
        final CountDownLatch largada = new CountDownLatch(1);
        final List<Throwable> errores = Collections.synchronizedList(new ArrayList<Throwable>());

        List<Thread> trabajadores = new ArrayList<Thread>();
        for (int i = 0; i < hilos; i++) {
            final boolean domicilio = (i % 2 == 0);
            Thread t = new Thread(() -> {
                try {
                    largada.await();
                    for (int j = 0; j < solicitudesPorHilo; j++) {
                        if (domicilio) {
                            ambulancia.atenderDomicilio(asociado);
                        } else {
                            ambulancia.trasladarALaClinica(asociado);
                        }
                        int actuales = dentro.incrementAndGet();
                        maxSimultaneos.accumulateAndGet(actuales, Math::max);
                        Thread.sleep(2);
                        dentro.decrementAndGet();
                        atendidas.incrementAndGet();
                        ambulancia.regresarSinPaciente();
                    }
                } catch (Throwable e) {
                    errores.add(e);
                }
            });
            trabajadores.add(t);
            t.start();
        }

        largada.countDown();
        for (Thread t : trabajadores) {
            t.join(25000);
            assertFalse(t.isAlive(), "Un hilo quedo bloqueado: posible deadlock o notificacion perdida.");
        }

        assertTrue(errores.isEmpty(), "Hubo errores en los hilos: " + errores);
        assertEquals(1, maxSimultaneos.get(), "Nunca debe haber mas de un asociado usando la ambulancia a la vez.");
        assertEquals(hilos * solicitudesPorHilo, atendidas.get(), "Todas las solicitudes deben ser atendidas.");
        assertFalse(ambulancia.isOcupado(), "Al terminar, la ambulancia debe quedar libre.");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testEstadoCoherenteEnCadaTrasladoConcurrente() throws Exception {
        final int hilos = 3;
        final int solicitudesPorHilo = 2;
        final AtomicInteger incoherencias = new AtomicInteger(0);
        final CountDownLatch largada = new CountDownLatch(1);

        List<Thread> trabajadores = new ArrayList<Thread>();
        for (int i = 0; i < hilos; i++) {
            Thread t = new Thread(() -> {
                try {
                    largada.await();
                    for (int j = 0; j < solicitudesPorHilo; j++) {
                        ambulancia.trasladarALaClinica(asociado);
                        // Mientras este hilo tiene la ambulancia, el estado debe ser "Trasladando"
                        if (!(ambulancia.getEstadoActual() instanceof TrasladandoPacienteClinicaState)) {
                            incoherencias.incrementAndGet();
                        }
                        ambulancia.regresarSinPaciente();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            trabajadores.add(t);
            t.start();
        }

        largada.countDown();
        for (Thread t : trabajadores) {
            t.join(25000);
            assertFalse(t.isAlive(), "Un hilo quedo bloqueado.");
        }

        assertEquals(0, incoherencias.get(),
                "Traslados atendidos con la ambulancia en un estado incorrecto: " + incoherencias.get()
                        + " de " + (hilos * solicitudesPorHilo));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testOperarioEsperaMientrasAmbulanciaAtiendeAsociado() throws Exception {
        ambulancia.atenderDomicilio(asociado);

        Thread hiloOperario = new Thread(() -> ambulancia.solicitarMantenimiento(operario));
        hiloOperario.start();
        Concurrencia.esperarEstado(hiloOperario, Thread.State.WAITING);

        assertInstanceOf(AtendiendoEnDomicilioState.class, ambulancia.getEstadoActual(),
                "El operario no debe alterar el estado mientras la ambulancia esta ocupada.");

        ambulancia.regresarSinPaciente();
        hiloOperario.join(3000);

        assertFalse(hiloOperario.isAlive(), "El operario debe despertarse al liberarse la ambulancia.");
        assertTrue(ambulancia.isOcupado(), "El operario debe haber tomado la ambulancia.");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testFinDeSimulacionLiberaATodosLosHilosEnEspera() throws Exception {
        ambulancia.trasladarALaClinica(asociado);

        List<Thread> enEspera = new ArrayList<Thread>();
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> ambulancia.atenderDomicilio(asociado));
            enEspera.add(t);
            t.start();
        }
        for (Thread t : enEspera) {
            Concurrencia.esperarEstado(t, Thread.State.WAITING);
        }

        ambulancia.setSimulacionActiva(false);

        for (Thread t : enEspera) {
            t.join(2000);
            assertFalse(t.isAlive(), "Todos los hilos en espera deben terminar al finalizar la simulacion.");
        }
        assertInstanceOf(TrasladandoPacienteClinicaState.class, ambulancia.getEstadoActual(),
                "Los hilos liberados por el fin de la simulacion no deben cambiar el estado.");
    }

    // =========================================================================
    // 5. verificarYFinalizarSimulacion: valores limite de solicitudes pendientes
    // =========================================================================
    @Test
    public void testFinalizaCuandoTodosLosAsociadosTerminaron() {
        Domicilio dom = new Domicilio("Colon", 1, "Mar del Plata");
        new Asociado("A", "Uno", "11111111", "1", dom, 0, ambulancia);   // limite: 0 pendientes
        new Asociado("B", "Dos", "22222222", "2", dom, 0, ambulancia);

        ambulancia.verificarYFinalizarSimulacion();

        assertFalse(ambulancia.isSimulacionActiva(), "Sin solicitudes pendientes la simulacion debe finalizar.");
    }

    @Test
    public void testNoFinalizaSiUnAsociadoTieneUnaSolicitudPendiente() {
        Domicilio dom = new Domicilio("Colon", 1, "Mar del Plata");
        new Asociado("A", "Uno", "11111111", "1", dom, 0, ambulancia);
        new Asociado("B", "Dos", "22222222", "2", dom, 1, ambulancia);   // limite: 1 pendiente

        ambulancia.verificarYFinalizarSimulacion();

        assertTrue(ambulancia.isSimulacionActiva(), "Con al menos una solicitud pendiente la simulacion sigue activa.");
    }
}
