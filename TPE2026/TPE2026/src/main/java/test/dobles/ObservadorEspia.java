package test.dobles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import modelo.modeloAplicacion.NotificacionSimulacion;

/**
 * Doble de prueba (spy) que observa una Ambulancia y captura
 * todas las notificaciones emitidas.
 */
public class ObservadorEspia implements Observer {

    private final List<NotificacionSimulacion> notificaciones =
            Collections.synchronizedList(new ArrayList<NotificacionSimulacion>());

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof NotificacionSimulacion) {
            notificaciones.add((NotificacionSimulacion) arg);
        }
    }

    public int cantidadDeTipo(String tipo) {
        synchronized (notificaciones) {
            int n = 0;
            for (NotificacionSimulacion ns : notificaciones) {
                if (tipo.equals(ns.getTipo())) {
                    n++;
                }
            }
            return n;
        }
    }

    public boolean recibioTipo(String tipo) {
        return cantidadDeTipo(tipo) > 0;
    }

    public int total() {
        return notificaciones.size();
    }
}
