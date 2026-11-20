package com.carmotors.sgt.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para el DTO {@link ComparacionProveedor}.
 */
class ComparacionProveedorTest {

    @Test
    @DisplayName("Debe instanciar con constructor vacío y asignar campos vía setters")
    void testConstructorVacioYSetters() {
        ComparacionProveedor dto = new ComparacionProveedor();
        dto.setProveedorId(5);
        dto.setNombreProveedor("Repuestos Rápidos");
        dto.setCosto(new BigDecimal("15000.00"));
        dto.setTiempoEntregaDias(2);
        dto.setEsHabitual(true);
        dto.setPromedioEvaluacion(new BigDecimal("4.8"));
        dto.setCantidadEvaluaciones(6);

        assertEquals(5, dto.getProveedorId());
        assertEquals("Repuestos Rápidos", dto.getNombreProveedor());
        assertEquals(new BigDecimal("15000.00"), dto.getCosto());
        assertEquals(2, dto.getTiempoEntregaDias());
        assertTrue(dto.isEsHabitual());
        assertEquals(new BigDecimal("4.8"), dto.getPromedioEvaluacion());
        assertEquals(6, dto.getCantidadEvaluaciones());
    }

    @Test
    @DisplayName("Debe instanciar con constructor completo")
    void testConstructorCompleto() {
        ComparacionProveedor dto = new ComparacionProveedor(
                10, "Frenos del Sur", new BigDecimal("45000.00"), 4, false, null, 0);

        assertEquals(10, dto.getProveedorId());
        assertEquals("Frenos del Sur", dto.getNombreProveedor());
        assertEquals(new BigDecimal("45000.00"), dto.getCosto());
        assertEquals(4, dto.getTiempoEntregaDias());
        assertNull(dto.getPromedioEvaluacion());
        assertEquals(0, dto.getCantidadEvaluaciones());
    }

    @Test
    @DisplayName("Debe formatear toString correctamente cuando tiene evaluaciones")
    void testToStringConEvaluaciones() {
        ComparacionProveedor dto = new ComparacionProveedor(
                1, "Distribuidora Central", new BigDecimal("32000.00"), 1, true,
                new BigDecimal("4.5"), 3);

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("Distribuidora Central"));
        assertTrue(str.contains("$32000.00"));
        assertTrue(str.contains("1 días"));
        assertTrue(str.contains("Habitual: Sí"));
        assertTrue(str.contains("4.5 (3 eval)"));
    }

    @Test
    @DisplayName("Debe formatear toString como 'Sin evaluar' cuando no tiene evaluaciones")
    void testToStringSinEvaluaciones() {
        ComparacionProveedor dto = new ComparacionProveedor(
                2, "Importadora Global", new BigDecimal("50000.00"), 5, false, null, 0);

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("Sin evaluar"));
        assertTrue(str.contains("Habitual: No"));
    }
}
