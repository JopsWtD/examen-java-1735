package com.carmotors.sgt.service.state;

import com.carmotors.sgt.exception.TransicionEstadoInvalidaException;

/**
 * Fábrica sencilla para mapear los IDs de servicio_estados a estados State.
 */
public class EstadoOrdenFactory {

    private EstadoOrdenFactory() {
    }

    public static EstadoOrden desdeId(int estadoId) {
        if (estadoId == 1) {
            return new EstadoPendiente();
        }
        if (estadoId == 2) {
            return new EstadoEnProceso();
        }
        if (estadoId == 3) {
            return new EstadoCompletado();
        }
        if (estadoId == 4) {
            return new EstadoFacturado();
        }
        if (estadoId == 5) {
            return new EstadoEntregado();
        }
        if (estadoId == 6) {
            return new EstadoCancelada();
        }
        throw new TransicionEstadoInvalidaException(
                "No existe un estado de orden asociado al ID: " + estadoId + "."
        );
    }
}
