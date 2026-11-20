package com.carmotors.sgt.exception;

/**
 * Excepción lanzada cuando ocurre un error en la carga o validación
 * de la configuración de la aplicación (config.properties).
 *
 * <p>Extiende {@link CarMotorsException} para integrarse en la jerarquía
 * de excepciones de la aplicación, permitiendo un fallo controlado sin invocar {@code System.exit()}.</p>
 */
public class ConfiguracionException extends CarMotorsException {

    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
