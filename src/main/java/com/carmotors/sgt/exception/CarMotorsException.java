package com.carmotors.sgt.exception;

/**
 * Excepción base no chequeada del dominio CarMotors SGT.
 *
 * <p>Todas las excepciones de negocio, validación y configuración del sistema
 * extienden de esta clase. Extiende {@link RuntimeException} para no forzar
 * firmas con {@code throws} innecesarias en capas intermedias y evitar el uso
 * de {@code System.exit()} (RNF-12, RNF-23).</p>
 *
 * <h3>Convención de capas (RNF-12, RNF-23):</h3>
 * <ul>
 *   <li><b>Service:</b> Valida reglas de negocio y lanza excepciones del dominio con mensajes amigables en español.
 *       <pre>{@code
 *       if (clienteRepository.existePorDocumento(documento)) {
 *           throw new DatoDuplicadoException("Ya existe un cliente con el documento " + documento);
 *       }
 *       }</pre>
 *   </li>
 *   <li><b>Controller:</b> Captura {@code CarMotorsException} (y derivadas) mostrando mensajes comprensibles
 *       al usuario de consola sin abortar la aplicación.
 *       <pre>{@code
 *       try {
 *           clienteService.registrar(cliente);
 *           System.out.println("  [OK] Cliente registrado exitosamente.");
 *       } catch (CarMotorsException e) {
 *           System.out.println("  [!] Error: " + e.getMessage());
 *       }
 *       }</pre>
 *   </li>
 *   <li><b>Repository:</b> No toma decisiones de negocio ni imprime en consola; ante errores de SQL
 *       propaga o envuelve {@link java.sql.SQLException} sin filtrar trazas crudas ni dejar bloques {@code catch} vacíos.
 *   </li>
 *   <li><b>View:</b> Solo lee/imprime en consola usando {@code ConsoleReader} e {@code InputValidator}.
 *   </li>
 * </ul>
 */
public class CarMotorsException extends RuntimeException {

    public CarMotorsException(String mensaje) {
        super(mensaje);
    }

    public CarMotorsException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
