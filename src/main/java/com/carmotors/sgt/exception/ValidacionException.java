package com.carmotors.sgt.exception;

/**
 * Excepción lanzada cuando una entidad, operación o argumento viola una regla
 * de negocio o validación de estado (ej. técnico inactivo, stock insuficiente,
 * transición de estado no permitida, importe no positivo).
 */
public class ValidacionException extends CarMotorsException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    public ValidacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
