package com.carmotors.sgt.exception;

/**
 * Excepción lanzada cuando una orden intenta avanzar a un estado no permitido
 * según el ciclo de vida RN-06.
 */
public class TransicionEstadoInvalidaException extends CarMotorsException {

    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }

    public TransicionEstadoInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
