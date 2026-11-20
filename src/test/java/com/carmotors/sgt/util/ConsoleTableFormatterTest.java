package com.carmotors.sgt.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para el formateador tabular de consola {@link ConsoleTableFormatter}.
 */
class ConsoleTableFormatterTest {

    @Test
    @DisplayName("Construye bordes exactos, calcula anchos dinámicos y alinea columnas numéricas a la derecha")
    void construyeTablaConAlineacionYBordes() {
        String[] encabezados = {"ID", "Nombre", "Total"};
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"1", "Filtro de aceite", "$25000.00"});
        filas.add(new String[]{"12", "Bujía", "$4500.00"});

        String tabla = ConsoleTableFormatter.construirTabla(
                encabezados,
                filas,
                new boolean[]{true, false, true},
                40);

        String esperada = ""
                + "  +----+------------------+-----------+\n"
                + "  | ID | Nombre           | Total     |\n"
                + "  +----+------------------+-----------+\n"
                + "  |  1 | Filtro de aceite | $25000.00 |\n"
                + "  | 12 | Bujía            |  $4500.00 |\n"
                + "  +----+------------------+-----------+\n";
        assertEquals(esperada, tabla);
    }

    @Test
    @DisplayName("Maneja valores nulos, saltos de línea embebidos y filas incompletas sin fallar")
    void manejaNulosSaltosYFilasIncompletas() {
        String[] encabezados = {"ID", "Descripción", "Estado"};
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"5", "Línea 1\nLínea 2", null});
        filas.add(new String[]{"6"});

        String tabla = ConsoleTableFormatter.construirTabla(encabezados, filas, null, 40);

        assertTrue(tabla.contains("| 5  | Línea 1 Línea 2 |        |"));
        assertTrue(tabla.contains("| 6  |                 |        |"));
    }

    @Test
    @DisplayName("Trunca con puntos suspensivos las celdas que superan el ancho máximo configurado")
    void truncaCeldasLargas() {
        String[] encabezados = {"Servicio"};
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Revisión completa del sistema de frenos delanteros"});

        String tabla = ConsoleTableFormatter.construirTabla(encabezados, filas, null, 15);

        assertTrue(tabla.contains("| Revisión com... |"));
    }

    @Test
    @DisplayName("Soporta tabla sin filas y retorna vacío si los encabezados son nulos o vacíos")
    void soportaTablaVaciaYEncabezadosInvalidos() {
        assertEquals("", ConsoleTableFormatter.construirTabla(null, new ArrayList<>(), null, 40));
        assertEquals("", ConsoleTableFormatter.construirTabla(new String[0], new ArrayList<>(), null, 40));

        String soloCabecera = ConsoleTableFormatter.construirTabla(
                new String[]{"ID", "Nombre"},
                new ArrayList<>(),
                null,
                40);
        String esperada = ""
                + "  +----+--------+\n"
                + "  | ID | Nombre |\n"
                + "  +----+--------+\n"
                + "  +----+--------+\n";
        assertEquals(esperada, soloCabecera);
    }
}
