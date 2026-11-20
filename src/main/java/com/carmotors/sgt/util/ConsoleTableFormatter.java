package com.carmotors.sgt.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad centralizada para imprimir listados y reportes por consola en formato tabular ASCII.
 * Calcula dinámicamente el ancho de cada columna, permite alineación por columna y evita romper
 * la lectura ante textos extensos o valores {@code null}.
 */
public final class ConsoleTableFormatter {

    public static final int MAX_ANCHO_CELDA_DEFECTO = 40;
    private static final String MARGEN = "  ";

    private ConsoleTableFormatter() {
    }

    /**
     * Imprime una tabla con alineación izquierda en todas las columnas y ancho máximo por defecto.
     */
    public static void imprimirTabla(String[] encabezados, List<String[]> filas) {
        imprimirTabla(encabezados, filas, null, MAX_ANCHO_CELDA_DEFECTO);
    }

    /**
     * Imprime una tabla indicando qué columnas numéricas o monetarias se alinean a la derecha.
     */
    public static void imprimirTabla(String[] encabezados, List<String[]> filas, boolean[] alinearDerecha) {
        imprimirTabla(encabezados, filas, alinearDerecha, MAX_ANCHO_CELDA_DEFECTO);
    }

    /**
     * Imprime una tabla indicando alineación por columna y ancho máximo permitido por celda.
     */
    public static void imprimirTabla(
            String[] encabezados,
            List<String[]> filas,
            boolean[] alinearDerecha,
            int maxAnchoCelda) {
        String tabla = construirTabla(encabezados, filas, alinearDerecha, maxAnchoCelda);
        if (!tabla.isEmpty()) {
            System.out.print(tabla);
        }
    }

    /**
     * Construye la representación tabular completa con bordes ASCII portables.
     *
     * @param encabezados    títulos de las columnas
     * @param filas          filas de datos (pueden tener valores {@code null})
     * @param alinearDerecha banderas opcionales por columna; {@code true} alinea a la derecha
     * @param maxAnchoCelda  límite de caracteres por celda antes de truncar con {@code "..."}; si es menor que 4 no trunca
     * @return cadena lista para imprimir o {@code ""} si no hay encabezados válidos
     */
    public static String construirTabla(
            String[] encabezados,
            List<String[]> filas,
            boolean[] alinearDerecha,
            int maxAnchoCelda) {
        if (encabezados == null || encabezados.length == 0) {
            return "";
        }

        int columnas = encabezados.length;
        String[] encabezadosLimpios = new String[columnas];
        int[] anchos = new int[columnas];

        for (int c = 0; c < columnas; c++) {
            encabezadosLimpios[c] = normalizarCelda(encabezados[c], maxAnchoCelda);
            anchos[c] = encabezadosLimpios[c].length();
        }

        List<String[]> filasLimpias = new ArrayList<>();
        if (filas != null) {
            for (String[] filaOriginal : filas) {
                String[] filaLimpia = new String[columnas];
                for (int c = 0; c < columnas; c++) {
                    String bruto = (filaOriginal != null && c < filaOriginal.length) ? filaOriginal[c] : "";
                    filaLimpia[c] = normalizarCelda(bruto, maxAnchoCelda);
                    if (filaLimpia[c].length() > anchos[c]) {
                        anchos[c] = filaLimpia[c].length();
                    }
                }
                filasLimpias.add(filaLimpia);
            }
        }

        String borde = construirBorde(anchos);
        StringBuilder sb = new StringBuilder();
        sb.append(borde);
        appendFila(sb, encabezadosLimpios, anchos, null);
        sb.append(borde);
        for (String[] fila : filasLimpias) {
            appendFila(sb, fila, anchos, alinearDerecha);
        }
        sb.append(borde);
        return sb.toString();
    }

    private static String normalizarCelda(String valor, int maxAnchoCelda) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace('\r', ' ').replace('\n', ' ').trim();
        if (maxAnchoCelda >= 4 && limpio.length() > maxAnchoCelda) {
            return limpio.substring(0, maxAnchoCelda - 3) + "...";
        }
        return limpio;
    }

    private static String construirBorde(int[] anchos) {
        StringBuilder sb = new StringBuilder();
        sb.append(MARGEN).append('+');
        for (int ancho : anchos) {
            repetir(sb, '-', ancho + 2);
            sb.append('+');
        }
        sb.append('\n');
        return sb.toString();
    }

    private static void appendFila(
            StringBuilder sb,
            String[] celdas,
            int[] anchos,
            boolean[] alinearDerecha) {
        sb.append(MARGEN).append('|');
        for (int c = 0; c < anchos.length; c++) {
            String texto = celdas[c];
            int relleno = anchos[c] - texto.length();
            boolean derecha = alinearDerecha != null && c < alinearDerecha.length && alinearDerecha[c];
            sb.append(' ');
            if (derecha) {
                repetir(sb, ' ', relleno);
                sb.append(texto);
            } else {
                sb.append(texto);
                repetir(sb, ' ', relleno);
            }
            sb.append(" |");
        }
        sb.append('\n');
    }

    private static void repetir(StringBuilder sb, char caracter, int veces) {
        for (int i = 0; i < veces; i++) {
            sb.append(caracter);
        }
    }
}
