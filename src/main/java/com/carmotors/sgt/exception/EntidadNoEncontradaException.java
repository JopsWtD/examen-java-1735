package com.carmotors.sgt.exception;

/**
 * Excepción lanzada cuando una entidad requerida no existe en el sistema
 * (búsqueda por ID, documento, placa, código de lote, etc.).
 */
public class EntidadNoEncontradaException extends CarMotorsException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public EntidadNoEncontradaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
