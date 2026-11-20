package com.carmotors.sgt.service.state;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

/**
 * Implementación base para los estados concretos de una orden.
 * Centraliza la ejecución y validación de transiciones comunes.
 */
public abstract class EstadoOrdenBase implements EstadoOrden {

    @Override
    public EstadoOrden transicionarA(EstadoOrden destino) {
        if (destino != null && puedeTransicionarA(destino)) {
            return destino;
        }
        lanzarTransicionInvalida(destino);
        return this;
    }

    protected void lanzarTransicionInvalida(EstadoOrden destino) {
        String nombreDestino = destino == null ? "estado nulo" : destino.getNombre();
        throw new TransicionEstadoInvalidaException(
                "Transición de estado inválida: no se puede pasar de " + getNombre()
                        + " a " + nombreDestino + "."
        );
    }
}
