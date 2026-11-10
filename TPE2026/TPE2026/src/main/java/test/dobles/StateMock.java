package test.dobles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modelo.modeloDominio.ambulancia.State;

/**
 * Doble de prueba (mock) de la interfaz State.
 * Registra cada invocacion recibida y permite programar un comportamiento
 * opcional por metodo, simulando un estado concreto que todavia no existe.
 * Es thread-safe para poder usarse en pruebas de concurrencia.
 */
public class StateMock implements State {

    public static final String TRASLADO = "SolicitudDeTraslado";
    public static final String DOMICILIO = "SolicitudDeAtencionDomicilio";
    public static final String MANTENIMIENTO = "SolicitudMantenimiento";
    public static final String RETORNO = "RetornoClinica";

    private final List<String> llamadas = Collections.synchronizedList(new ArrayList<String>());
    private final Map<String, Runnable> comportamientos = Collections.synchronizedMap(new HashMap<String, Runnable>());

    /** Programa una accion a ejecutar cuando se invoque el metodo indicado. */
    public void programar(String metodo, Runnable accion) {
        comportamientos.put(metodo, accion);
    }

    /** Cantidad de veces que se invoco el metodo indicado. */
    public int veces(String metodo) {
        synchronized (llamadas) {
            int n = 0;
            for (String l : llamadas) {
                if (l.equals(metodo)) {
                    n++;
                }
            }
            return n;
        }
    }

    /** Copia de todas las invocaciones en orden. */
    public List<String> llamadas() {
        synchronized (llamadas) {
            return new ArrayList<String>(llamadas);
        }
    }

    public int totalLlamadas() {
        return llamadas.size();
    }

    private void registrar(String metodo) {
        llamadas.add(metodo);
        Runnable accion = comportamientos.get(metodo);
        if (accion != null) {
            accion.run();
        }
    }

    @Override
    public void SolicitudDeTraslado() {
        registrar(TRASLADO);
    }

    @Override
    public void SolicitudDeAtencionDomicilio() {
        registrar(DOMICILIO);
    }

    @Override
    public void SolicitudMantenimiento() {
        registrar(MANTENIMIENTO);
    }

    @Override
    public void RetornoClinica() {
        registrar(RETORNO);
    }
}
