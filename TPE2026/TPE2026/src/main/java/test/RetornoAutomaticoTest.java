package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.DisponibleState;
import modelo.modeloDominio.ambulancia.RegresandoDelTallerState;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;
import test.dobles.AmbulanciaStub;

/**
 * Pruebas del hilo RetornoAutomatico.
 * Las primeras aislan el hilo con un stub de Ambulancia; la ultima lo integra
 * con una Ambulancia y estados reales.
 * Nota: el intervalo del hilo es fijo (10 s), por eso algunas pruebas tardan ~10 s.
 */
public class RetornoAutomaticoTest {

    private static final long INTERVALO_MS = 10000;
    private static final long MARGEN_MS = 3000;

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testConSimulacionActivaEjecutaElRetorno() throws Exception {
        // Activa durante una vuelta del ciclo y luego finaliza
        AmbulanciaStub stub = new AmbulanciaStub(false, true, true);
        Thread hilo = new Thread(new RetornoAutomatico(stub));

        hilo.start();
        hilo.join(INTERVALO_MS + MARGEN_MS);

        assertFalse(hilo.isAlive(), "El hilo debe terminar cuando la simulacion finaliza.");
        assertEquals(1, stub.cantidadRetornos(),
                "Con la simulacion activa el hilo debe invocar retornoAutomatico una vez por intervalo.");
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testConSimulacionInactivaTerminaInmediatamente() throws Exception {
        AmbulanciaStub stub = new AmbulanciaStub(false);
        Thread hilo = new Thread(new RetornoAutomatico(stub));

        hilo.start();
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "Con la simulacion inactiva el hilo debe terminar sin esperar el intervalo.");
        assertEquals(0, stub.cantidadRetornos(), "Con la simulacion inactiva no debe haber retornos.");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testInterrupcionTerminaElHiloSinRetornar() throws Exception {
        AmbulanciaStub stub = new AmbulanciaStub(true);
        Thread hilo = new Thread(new RetornoAutomatico(stub));

        hilo.start();
        Thread.sleep(200);
        hilo.interrupt();
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "El hilo debe terminar limpiamente al ser interrumpido.");
        assertEquals(0, stub.cantidadRetornos(), "Interrumpido durante la espera, no debe ejecutar el retorno.");
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testIntegracionRetornoAutomaticoDevuelveAmbulanciaADisponible() throws Exception {
        Ambulancia ambulancia = new Ambulancia();
        ambulancia.setState(new RegresandoDelTallerState(ambulancia));
        Thread hilo = new Thread(new RetornoAutomatico(ambulancia));

        hilo.start();
        long limite = System.currentTimeMillis() + INTERVALO_MS + MARGEN_MS;
        while (!(ambulancia.getEstadoActual() instanceof DisponibleState) && System.currentTimeMillis() < limite) {
            Thread.sleep(100);
        }

        ambulancia.setSimulacionActiva(false);
        hilo.interrupt();
        hilo.join(2000);

        assertInstanceOf(DisponibleState.class, ambulancia.getEstadoActual(),
                "El retorno automatico debe llevar a la ambulancia de RegresandoDelTaller a Disponible.");
    }
}
