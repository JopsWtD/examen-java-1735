package com.carmotors.sgt.config;

import com.carmotors.sgt.exception.ConfiguracionException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link ConnectionFactory} y {@link ConfiguracionException}.
 */
class ConnectionFactoryTest {

    @Test
    @DisplayName("El Singleton debe devolver la misma instancia si hay configuración válida, o lanzar ConfiguracionException legible")
    void testSingletonOConfiguracionException() {
        try {
            ConnectionFactory primera = ConnectionFactory.getInstance();
            ConnectionFactory segunda = ConnectionFactory.getInstance();

            assertNotNull(primera, "La instancia no debe ser nula");
            assertSame(primera, segunda, "Ambas llamadas a getInstance() deben retornar la misma instancia en memoria");

            String carpeta = primera.getCarpetaFacturas();
            assertNotNull(carpeta, "La carpeta de facturas no debe ser nula");
            assertTrue(!carpeta.trim().isEmpty(), "La carpeta de facturas no debe estar vacía");

        } catch (ConfiguracionException e) {
            assertNotNull(e.getMessage(), "El mensaje de la excepción no debe ser nulo");
            assertTrue(e.getMessage().contains("config.properties"),
                    "El mensaje de error debe orientar al usuario indicando el archivo 'config.properties'");
        }
    }

    @Test
    @DisplayName("ConfiguracionException debe conservar el mensaje y la causa raíz")
    void testConfiguracionExceptionConstructores() {
        ConfiguracionException exSimple = new ConfiguracionException("Mensaje de prueba");
        assertEquals("Mensaje de prueba", exSimple.getMessage());

        Throwable causa = new IllegalArgumentException("Causa raíz");
        ConfiguracionException exConCausa = new ConfiguracionException("Error con causa", causa);
        assertEquals("Error con causa", exConCausa.getMessage());
        assertSame(causa, exConCausa.getCause());
    }

    @Test
    @DisplayName("Verificación de conexión con SELECT 1 si existe configuración local y MySQL está disponible")
    void testConexionBdCondicional() {
        File archivoLocal = new File("src/main/resources/config.properties");
        Assumptions.assumeTrue(archivoLocal.exists(),
                "Omitiendo verificación en vivo: no existe 'src/main/resources/config.properties' local.");

        try (Connection conn = ConnectionFactory.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            assertTrue(rs.next(), "La consulta SELECT 1 debe retornar al menos una fila");
            assertEquals(1, rs.getInt(1), "El valor retornado por SELECT 1 debe ser 1");

        } catch (SQLException e) {
            Assumptions.assumeTrue(false,
                    "Omitiendo prueba: MySQL no está accesible en este momento (" + e.getMessage() + ")");
        } catch (ConfiguracionException e) {
            Assumptions.assumeTrue(false,
                    "Omitiendo prueba: Configuración incompleta (" + e.getMessage() + ")");
        }
    }
}
