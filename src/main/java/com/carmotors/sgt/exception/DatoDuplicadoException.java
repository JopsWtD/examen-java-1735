package com.carmotors.sgt.exception;

/**
 * Excepción lanzada cuando se intenta registrar o actualizar una entidad
 * con un dato único que ya existe en la base de datos (documento, placa, email, código de lote, etc.).
 */
public class DatoDuplicadoException extends CarMotorsException {

    public DatoDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public DatoDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
