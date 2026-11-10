package test;

/**
 * Utilidades para sincronizar los tests con hilos en espera,
 * sin depender de sleeps fijos.
 */
final class Concurrencia {

    private Concurrencia() {
    }

    /** Espera (hasta 2 s) a que el hilo alcance el estado indicado. */
    static void esperarEstado(Thread hilo, Thread.State estado) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (hilo.getState() != estado) {
            if (System.currentTimeMillis() > limite) {
                throw new AssertionError("El hilo no llego al estado " + estado + " (estado actual: " + hilo.getState() + ")");
            }
            Thread.sleep(5);
        }
    }
}
