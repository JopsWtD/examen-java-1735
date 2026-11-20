package com.carmotors.sgt.exception;

/**
 * Excepción lanzada cuando la cantidad solicitada de un repuesto supera el
 * disponible para nuevas reservas (RN-01, RNF-12). El mensaje informa el disponible.
 */
public class StockInsuficienteException extends CarMotorsException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }

    public StockInsuficienteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
