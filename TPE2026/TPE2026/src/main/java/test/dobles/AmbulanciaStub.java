package test.dobles;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import modelo.modeloDominio.ambulancia.Ambulancia;
import modelo.modeloDominio.ambulancia.RetornoAutomatico;

/**
 * Doble de prueba (stub + spy) de Ambulancia para aislar al hilo RetornoAutomatico.
 * isSimulacionActiva() devuelve una secuencia guionada de valores y, agotada
 * la secuencia, devuelve el valor por defecto. retornoAutomatico() solo cuenta
 * invocaciones, sin tocar la maquina de estados real.
 */
public class AmbulanciaStub extends Ambulancia {

    private final LinkedList<Boolean> guion = new LinkedList<Boolean>();
    private final boolean valorPorDefecto;
    private final AtomicInteger retornos = new AtomicInteger(0);

    public AmbulanciaStub(boolean valorPorDefecto, Boolean... secuencia) {
        super();
        this.valorPorDefecto = valorPorDefecto;
        for (Boolean b : secuencia) {
            guion.add(b);
        }
    }

    @Override
    public synchronized boolean isSimulacionActiva() {
        return guion.isEmpty() ? valorPorDefecto : guion.poll();
    }

    @Override
    public synchronized void retornoAutomatico(RetornoAutomatico retornoAutomatico) {
        retornos.incrementAndGet();
    }

    public int cantidadRetornos() {
        return retornos.get();
    }
}
