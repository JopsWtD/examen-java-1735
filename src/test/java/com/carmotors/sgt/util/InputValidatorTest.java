package com.carmotors.sgt.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para validadores de formato lógico en {@link InputValidator}
 * y lectores con repregunta en {@link ConsoleReader}.
 */
class InputValidatorTest {

    @AfterEach
    void tearDown() {
        ConsoleReader.reiniciarScannerSistema();
    }

    @Test
    @DisplayName("esNombrePersonaValido exige 2 a 4 palabras alfabéticas sin números ni símbolos")
    void validarNombrePersona() {
        assertTrue(InputValidator.esNombrePersonaValido("Pedro Ramírez"));
        assertTrue(InputValidator.esNombrePersonaValido("María Fernanda Rojas"));
        assertTrue(InputValidator.esNombrePersonaValido("Juan Carlos Pérez López"));
        assertTrue(InputValidator.esNombrePersonaValido("  Ana   Muñoz  "));

        assertFalse(InputValidator.esNombrePersonaValido("0"));
        assertFalse(InputValidator.esNombrePersonaValido("12345"));
        assertFalse(InputValidator.esNombrePersonaValido("Pedro"));
        assertFalse(InputValidator.esNombrePersonaValido("Pedro 123"));
        assertFalse(InputValidator.esNombrePersonaValido("A B"));
        assertFalse(InputValidator.esNombrePersonaValido("Uno Dos Tres Cuatro Cinco"));
        assertFalse(InputValidator.esNombrePersonaValido("Pedro-Ramírez"));
        assertFalse(InputValidator.esNombrePersonaValido(""));
    }

    @Test
    @DisplayName("esNombreClienteValido acepta personas naturales y razones sociales sin dígitos")
    void validarNombreCliente() {
        assertTrue(InputValidator.esNombreClienteValido("María Fernanda Rojas"));
        assertTrue(InputValidator.esNombreClienteValido("Pablo Ortiz"));
        assertTrue(InputValidator.esNombreClienteValido("Transportes Rápidos S.A.S."));

        assertFalse(InputValidator.esNombreClienteValido("0"));
        assertFalse(InputValidator.esNombreClienteValido("123456"));
        assertFalse(InputValidator.esNombreClienteValido("Carlos"));
        assertFalse(InputValidator.esNombreClienteValido("Empresa 2026"));
        assertFalse(InputValidator.esNombreClienteValido("--- ---"));
    }

    @Test
    @DisplayName("Validadores de documento, NIT, teléfono y código de lote rechazan valores irrisorios")
    void validarDocumentoTelefonoYLote() {
        assertTrue(InputValidator.esDocumentoNumericoValido("123456"));
        assertTrue(InputValidator.esDocumentoNumericoValido("1090456123"));
        assertFalse(InputValidator.esDocumentoNumericoValido("2"));
        assertFalse(InputValidator.esDocumentoNumericoValido("12345"));
        assertFalse(InputValidator.esDocumentoNumericoValido("900123456-7"));
        assertFalse(InputValidator.esDocumentoNumericoValido("abc12345"));

        assertTrue(InputValidator.esDocumentoClienteValido("88245604"));
        assertTrue(InputValidator.esDocumentoClienteValido("900123456-7"));
        assertFalse(InputValidator.esDocumentoClienteValido("2"));
        assertFalse(InputValidator.esDocumentoClienteValido("900123456-78"));

        assertTrue(InputValidator.esTelefonoValido("1234567"));
        assertTrue(InputValidator.esTelefonoValido("3205550001"));
        assertFalse(InputValidator.esTelefonoValido("2"));
        assertFalse(InputValidator.esTelefonoValido("123456"));
        assertFalse(InputValidator.esTelefonoValido("300abc1234"));

        assertTrue(InputValidator.esCodigoLoteValido("LOT-ACE-2026-001"));
        assertTrue(InputValidator.esCodigoLoteValido("L01"));
        assertFalse(InputValidator.esCodigoLoteValido("0"));
        assertFalse(InputValidator.esCodigoLoteValido("12345"));
        assertFalse(InputValidator.esCodigoLoteValido("LOT 001"));
    }

    @Test
    @DisplayName("ConsoleReader repregunta ante entradas inválidas como '0' o '2' hasta recibir datos válidos")
    void consoleReaderRepreguntaDatosPersonaDocumentoYTelefono() {
        String entradas = String.join("\n",
                "0",
                "Pedro",
                "Pedro   Ramírez",
                "2",
                "abc",
                "1090456123",
                "2",
                "3205550001",
                "0",
                "Transportes Rápidos S.A.S.",
                "2",
                "900123456-7"
        ) + "\n";
        ConsoleReader.setScanner(new Scanner(entradas));

        assertEquals("Pedro Ramírez", ConsoleReader.leerNombrePersona("Nombre"));
        assertEquals("1090456123", ConsoleReader.leerDocumentoNumerico("Documento"));
        assertEquals("3205550001", ConsoleReader.leerTelefonoOpcional("Teléfono"));
        assertEquals("Transportes Rápidos S.A.S.", ConsoleReader.leerNombreCliente("Cliente"));
        assertEquals("900123456-7", ConsoleReader.leerDocumentoCliente("NIT"));
    }

    @Test
    @DisplayName("leerHorasNoNegativas informa error hablando de horas y nunca de importe")
    void leerHorasNoNegativasMuestraMensajeDeHoras() {
        ConsoleReader.setScanner(new Scanner("-2\n1.5\n"));
        PrintStream salidaOriginal = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        BigDecimal resultado;
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            resultado = ConsoleReader.leerHorasNoNegativas("Horas trabajadas");
        } finally {
            System.setOut(salidaOriginal);
        }

        String salida = buffer.toString(StandardCharsets.UTF_8).toLowerCase();
        assertTrue(salida.contains("horas"));
        assertFalse(salida.contains("importe"));
        assertEquals(new BigDecimal("1.50"), resultado);
    }
}
