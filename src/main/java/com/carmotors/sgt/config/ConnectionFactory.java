package com.carmotors.sgt.config;

import com.carmotors.sgt.exception.ConfiguracionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica de conexiones JDBC y punto central de acceso a la configuración externa.
 *
 * <p>Aplica el patrón Creacional <b>Singleton</b>: la configuración de {@code config.properties}
 * se carga una única vez en memoria para optimizar el rendimiento y evitar lecturas repetidas de disco.
 * Sin embargo, el método {@link #getConnection()} entrega una <b>nueva</b> conexión JDBC
 * en cada llamada, permitiendo que cada operación o servicio maneje su propia transacción
 * atómica con {@code setAutoCommit(false)}, {@code commit()} y {@code rollback()}.</p>
 */
public final class ConnectionFactory {

    private static final String ARCHIVO_CONFIG = "config.properties";

    private static ConnectionFactory instancia;

    private final Properties propiedades;

    /**
     * Constructor privado para impedir la instanciación directa desde fuera de la clase.
     * Carga y valida las propiedades obligatorias al inicializarse.
     */
    private ConnectionFactory() {
        this.propiedades = cargarPropiedades();
        validarConfiguracion();
    }

    /**
     * Obtiene la única instancia de la fábrica mediante inicialización perezosa (Lazy Loading).
     * El método es {@code synchronized} para garantizar seguridad entre hilos concurrentes.
     *
     * @return la instancia única de {@link ConnectionFactory}
     */
    public static synchronized ConnectionFactory getInstance() {
        if (instancia == null) {
            instancia = new ConnectionFactory();
        }
        return instancia;
    }

    /**
     * Crea y devuelve una nueva conexión física a la base de datos MySQL.
     * Quien invoque este método es responsable de cerrar la conexión (por ejemplo con try-with-resources).
     *
     * @return una nueva instancia de {@link Connection}
     * @throws SQLException si ocurre un error al conectar con el servidor MySQL
     */
    public Connection getConnection() throws SQLException {
        String url = obtenerRequerida("db.url");
        String user = obtenerRequerida("db.user");
        String password = propiedades.getProperty("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Retorna la ruta de la carpeta configurada para guardar las facturas en texto plano.
     *
     * @return ruta del directorio de facturas (ej. "output")
     */
    public String getCarpetaFacturas() {
        return obtenerRequerida("factura.carpeta");
    }

    /**
     * Carga el archivo {@code config.properties} desde el classpath en formato UTF-8.
     *
     * @return objeto {@link Properties} con las claves cargadas
     */
    private Properties cargarPropiedades() {
        InputStream entrada = ConnectionFactory.class.getClassLoader().getResourceAsStream(ARCHIVO_CONFIG);
        if (entrada == null) {
            throw new ConfiguracionException(
                    "No se encontró '" + ARCHIVO_CONFIG + "' en el classpath. "
                    + "Copia src/main/resources/config.properties.example como "
                    + "src/main/resources/config.properties y ajusta tus credenciales de MySQL.");
        }

        try (Reader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(lector);
            return props;
        } catch (IOException e) {
            throw new ConfiguracionException("Error de entrada/salida al leer '" + ARCHIVO_CONFIG + "'.", e);
        }
    }

    /**
     * Valida que las propiedades esenciales del sistema existan y no estén vacías.
     */
    private void validarConfiguracion() {
        obtenerRequerida("db.url");
        obtenerRequerida("db.user");
        obtenerRequerida("factura.carpeta");
    }

    /**
     * Obtiene una propiedad obligatoria y asegura que no sea nula ni contenga solo espacios en blanco.
     *
     * @param clave nombre de la propiedad
     * @return valor de la propiedad sin espacios en los extremos
     */
    private String obtenerRequerida(String clave) {
        String valor = propiedades.getProperty(clave);
        if (valor == null || valor.trim().isEmpty()) {
            throw new ConfiguracionException(
                    "Falta la propiedad obligatoria '" + clave + "' en " + ARCHIVO_CONFIG + ".");
        }
        return valor.trim();
    }
}
