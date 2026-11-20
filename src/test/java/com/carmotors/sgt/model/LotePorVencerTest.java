package com.carmotors.sgt.model;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LotePorVencerTest {

    @Test
    @DisplayName("Debe instanciar y asignar atributos mediante constructor completo y getters")
    void constructorCompletoYGetters() {
        Lote lote = new Lote(1, "LOT-001", 2, 3, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 1), 10, 5);
        LotePorVencer dto = new LotePorVencer(lote, "Filtro", "Proveedor A", "POR VENCER", 15);

        assertNotNull(dto.getLote());
        assertEquals("LOT-001", dto.getLote().getCodigoLote());
        assertEquals("Filtro", dto.getNombreRepuesto());
        assertEquals("Proveedor A", dto.getNombreProveedor());
        assertEquals("POR VENCER", dto.getEstado());
        assertEquals(15, dto.getDiasRestantes());
    }

    @Test
    @DisplayName("Debe permitir asignación mediante constructor vacío y setters")
    void constructorVacioYSetters() {
        LotePorVencer dto = new LotePorVencer();
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-VENC");

        dto.setLote(lote);
        dto.setNombreRepuesto("Bujía");
        dto.setNombreProveedor("Proveedor B");
        dto.setEstado("VENCIDO");
        dto.setDiasRestantes(-5);

        assertEquals("LOT-VENC", dto.getLote().getCodigoLote());
        assertEquals("Bujía", dto.getNombreRepuesto());
        assertEquals("Proveedor B", dto.getNombreProveedor());
        assertEquals("VENCIDO", dto.getEstado());
        assertEquals(-5, dto.getDiasRestantes());
    }

    @Test
    @DisplayName("Debe generar toString representativo sin fallar con nulos")
    void toStringValido() {
        LotePorVencer dto = new LotePorVencer(null, "Aceite", "Proveedor C", "POR VENCER", 3);
        String representation = dto.toString();
        assertTrue(representation.contains("Aceite"));
        assertTrue(representation.contains("Proveedor C"));
        assertTrue(representation.contains("POR VENCER"));
    }
}
