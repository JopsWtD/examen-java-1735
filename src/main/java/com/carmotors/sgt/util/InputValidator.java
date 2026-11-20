package com.carmotors.sgt.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilidades de validación, normalización y conversión de formatos para entradas de consola.
 * Cumple con RNF-13 (validación de formatos) y RNF-15 (dinero con {@link BigDecimal} escala 2 y HALF_UP).
 */
public final class InputValidator {

    public static final int ESCALA_MONETARIA = 2;
    public static final RoundingMode REDONDEO_MONETARIO = RoundingMode.HALF_UP;
    public static final BigDecimal MAX_MONTO_DECIMAL_12_2 = new BigDecimal("9999999999.99");
    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final String REGEX_EMAIL =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    private static final String REGEX_PLACA = "^[A-Z0-9]{5,10}$";
    private static final String REGEX_PALABRA_NOMBRE_PERSONA = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]{2,}$";
    private static final String REGEX_PALABRA_NOMBRE_CLIENTE = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü][A-Za-zÁÉÍÓÚáéíóúÑñÜü.]*$";
    private static final String REGEX_DOCUMENTO_NUMERICO = "^[0-9]{6,12}$";
    private static final String REGEX_DOCUMENTO_O_NIT = "^([0-9]{6,12}|[0-9]{6,12}-[0-9])$";
    private static final String REGEX_TELEFONO = "^[0-9]{7,15}$";
    private static final String REGEX_CODIGO_LOTE = "^[A-Za-z0-9_-]{3,40}$";

    private InputValidator() {
    }

    /**
     * Recorta espacios al inicio y al final; retorna cadena vacía si la entrada es {@code null}.
     */
    public static String normalizarTexto(String entrada) {
        if (entrada == null) {
            return "";
        }
        return entrada.trim();
    }

    /**
     * Normaliza un texto para búsquedas en memoria: recorta espacios, convierte a minúsculas
     * y quita tildes y diéresis comunes en español.
     */
    public static String normalizarParaBusqueda(String entrada) {
        String texto = normalizarTexto(entrada).toLowerCase();
        return texto
                .replace('á', 'a')
                .replace('é', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ú', 'u')
                .replace('ü', 'u');
    }

    /**
     * Verifica si la entrada contiene al menos un carácter visible tras aplicar {@code trim()}.
     */
    public static boolean esTextoNoVacio(String entrada) {
        return !normalizarTexto(entrada).isEmpty();
    }

    /**
     * Recorta extremos y colapsa múltiples espacios internos a un único espacio.
     */
    public static String normalizarEspacios(String entrada) {
        String texto = normalizarTexto(entrada);
        if (texto.isEmpty()) {
            return "";
        }
        return texto.replaceAll("\\s+", " ");
    }

    /**
     * Indica si el texto contiene al menos una letra alfabética.
     */
    public static boolean contieneLetra(String entrada) {
        String texto = normalizarTexto(entrada);
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isLetter(texto.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si el texto contiene al menos una letra o un dígito.
     */
    public static boolean contieneAlfanumerico(String entrada) {
        String texto = normalizarTexto(entrada);
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isLetterOrDigit(texto.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si el texto contiene algún dígito numérico ({@code 0-9}).
     */
    public static boolean contieneDigito(String entrada) {
        String texto = normalizarTexto(entrada);
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isDigit(texto.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valida un nombre completo de persona natural (técnicos): entre 2 y 4 palabras
     * únicamente con letras (incluyendo tildes y ñ), sin dígitos ni símbolos, mínimo 2 letras por palabra.
     */
    public static boolean esNombrePersonaValido(String entrada) {
        String normalizado = normalizarEspacios(entrada);
        if (normalizado.isEmpty() || normalizado.length() > MAX_LONGITUD_NOMBRE) {
            return false;
        }
        String[] palabras = normalizado.split(" ");
        if (palabras.length < 2 || palabras.length > 4) {
            return false;
        }
        for (String palabra : palabras) {
            if (!palabra.matches(REGEX_PALABRA_NOMBRE_PERSONA)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valida el nombre de un cliente (persona natural con nombre y apellido o razón social como {@code S.A.S.}):
     * mínimo 2 palabras, sin dígitos, letras/tildes y puntos solo en abreviaturas/siglas.
     */
    public static boolean esNombreClienteValido(String entrada) {
        String normalizado = normalizarEspacios(entrada);
        if (normalizado.isEmpty() || normalizado.length() > MAX_LONGITUD_NOMBRE || contieneDigito(normalizado)) {
            return false;
        }
        String[] palabras = normalizado.split(" ");
        if (palabras.length < 2) {
            return false;
        }
        int palabrasConLetras = 0;
        for (String palabra : palabras) {
            if (!palabra.matches(REGEX_PALABRA_NOMBRE_CLIENTE)) {
                return false;
            }
            String soloLetras = palabra.replace(".", "");
            if (soloLetras.length() < 2) {
                return false;
            }
            palabrasConLetras++;
        }
        return palabrasConLetras >= 2;
    }

    /**
     * Valida un documento de identidad numérico de persona natural (6 a 12 dígitos).
     */
    public static boolean esDocumentoNumericoValido(String entrada) {
        return normalizarTexto(entrada).matches(REGEX_DOCUMENTO_NUMERICO);
    }

    /**
     * Valida un documento de cliente: cédula de 6 a 12 dígitos o NIT con dígito de verificación ({@code 900123456-7}).
     */
    public static boolean esDocumentoClienteValido(String entrada) {
        return normalizarTexto(entrada).matches(REGEX_DOCUMENTO_O_NIT);
    }

    /**
     * Valida un teléfono compuesto exclusivamente por 7 a 15 dígitos numéricos.
     */
    public static boolean esTelefonoValido(String entrada) {
        return normalizarTexto(entrada).matches(REGEX_TELEFONO);
    }

    /**
     * Valida un código de lote: 3 a 40 caracteres alfanuméricos, guiones o guion bajo, sin espacios y con al menos una letra.
     */
    public static boolean esCodigoLoteValido(String entrada) {
        String texto = normalizarTexto(entrada);
        return texto.matches(REGEX_CODIGO_LOTE) && contieneLetra(texto);
    }

    /**
     * Verifica si la cadena representa un número entero válido.
     */
    public static boolean esEntero(String entrada) {
        String texto = normalizarTexto(entrada);
        if (texto.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(texto);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Convierte una cadena válida en entero.
     */
    public static int convertirAEntero(String entrada) {
        return Integer.parseInt(normalizarTexto(entrada));
    }

    /**
     * Verifica que el entero sea estrictamente positivo ({@code > 0}).
     */
    public static boolean esEnteroPositivo(int valor) {
        return valor > 0;
    }

    /**
     * Verifica que el entero sea mayor o igual a cero ({@code >= 0}).
     */
    public static boolean esEnteroNoNegativo(int valor) {
        return valor >= 0;
    }

    /**
     * Verifica que el entero se encuentre dentro del rango inclusivo {@code [min, max]}.
     */
    public static boolean esEnteroEnRango(int valor, int min, int max) {
        return valor >= min && valor <= max;
    }

    /**
     * Verifica si la cadena representa un número decimal válido para {@link BigDecimal}.
     */
    public static boolean esBigDecimalValido(String entrada) {
        String texto = normalizarTexto(entrada);
        if (texto.isEmpty()) {
            return false;
        }
        String normalizado = texto.replace(',', '.');
        try {
            new BigDecimal(normalizado).setScale(ESCALA_MONETARIA, REDONDEO_MONETARIO);
            return true;
        } catch (NumberFormatException | ArithmeticException e) {
            return false;
        }
    }

    /**
     * Convierte una cadena numérica válida a {@link BigDecimal} con escala 2 y {@link RoundingMode#HALF_UP}.
     */
    public static BigDecimal convertirABigDecimal(String entrada) {
        String normalizado = normalizarTexto(entrada).replace(',', '.');
        return new BigDecimal(normalizado).setScale(ESCALA_MONETARIA, REDONDEO_MONETARIO);
    }

    /**
     * Verifica que el monto sea no nulo y estrictamente positivo ({@code > 0}).
     */
    public static boolean esBigDecimalPositivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica que el monto sea no nulo y mayor o igual a cero ({@code >= 0}).
     */
    public static boolean esBigDecimalNoNegativo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Valida que la fecha tenga formato {@code yyyy-MM-dd} y corresponda a una fecha real del calendario.
     */
    public static boolean esFechaValida(String entrada) {
        String texto = normalizarTexto(entrada);
        if (texto.isEmpty()) {
            return false;
        }
        try {
            LocalDate fecha = LocalDate.parse(texto, FORMATO_FECHA);
            return fecha.toString().equals(texto);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Convierte una cadena válida en formato {@code yyyy-MM-dd} a {@link LocalDate}.
     */
    public static LocalDate convertirAFecha(String entrada) {
        return LocalDate.parse(normalizarTexto(entrada), FORMATO_FECHA);
    }

    /**
     * Valida que la cadena corresponda a un formato estándar de correo electrónico.
     */
    public static boolean esEmailValido(String entrada) {
        String texto = normalizarTexto(entrada);
        return !texto.isEmpty() && texto.matches(REGEX_EMAIL);
    }

    /**
     * Normaliza la placa vehicular eliminando espacios y guiones y pasándola a mayúsculas (RN-13).
     */
    public static String normalizarPlaca(String entrada) {
        if (entrada == null) {
            return "";
        }
        return entrada.replace(" ", "").replace("-", "").toUpperCase();
    }

    /**
     * Verifica que la placa normalizada tenga entre 5 y 10 caracteres alfanuméricos.
     */
    public static boolean esPlacaValida(String entrada) {
        String placa = normalizarPlaca(entrada);
        return placa.matches(REGEX_PLACA);
    }

    /**
     * Verifica si la respuesta corresponde a una confirmación válida Sí/No.
     */
    public static boolean esConfirmacionValida(String entrada) {
        String valor = normalizarTexto(entrada).toUpperCase();
        return valor.equals("S") || valor.equals("SI") || valor.equals("SÍ")
                || valor.equals("Y") || valor.equals("YES")
                || valor.equals("N") || valor.equals("NO");
    }

    /**
     * Convierte una respuesta válida Sí/No en {@code true} (Sí) o {@code false} (No).
     */
    public static boolean convertirAConfirmacion(String entrada) {
        String valor = normalizarTexto(entrada).toUpperCase();
        return valor.equals("S") || valor.equals("SI") || valor.equals("SÍ")
                || valor.equals("Y") || valor.equals("YES");
    }
}
