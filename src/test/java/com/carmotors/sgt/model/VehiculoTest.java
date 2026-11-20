package com.carmotors.sgt.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para el modelo {@link Vehiculo}.
 */
class VehiculoTest {

    @Test
    @DisplayName("Debe instanciar con constructor vacío y asignar atributos vía setters")
    void testConstructorVacioYSetters() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(10);
        vehiculo.setClienteId(2);
        vehiculo.setPlaca("ABC123");
        vehiculo.setMarca("Toyota");
        vehiculo.setModelo("Corolla");

        assertEquals(10, vehiculo.getId());
        assertEquals(2, vehiculo.getClienteId());
        assertEquals("ABC123", vehiculo.getPlaca());
        assertEquals("Toyota", vehiculo.getMarca());
        assertEquals("Corolla", vehiculo.getModelo());
    }

    @Test
    @DisplayName("Debe instanciar con constructor de 4 parámetros sin ID")
    void testConstructorSinId() {
        Vehiculo vehiculo = new Vehiculo(3, "XYZ789", "Mazda", "3");

        assertEquals(0, vehiculo.getId());
        assertEquals(3, vehiculo.getClienteId());
        assertEquals("XYZ789", vehiculo.getPlaca());
        assertEquals("Mazda", vehiculo.getMarca());
        assertEquals("3", vehiculo.getModelo());
    }

    @Test
    @DisplayName("Debe instanciar con constructor completo de 5 parámetros")
    void testConstructorCompleto() {
        Vehiculo vehiculo = new Vehiculo(5, 1, "KTM999", "Chevrolet", "Onix");

        assertEquals(5, vehiculo.getId());
        assertEquals(1, vehiculo.getClienteId());
        assertEquals("KTM999", vehiculo.getPlaca());
        assertEquals("Chevrolet", vehiculo.getMarca());
        assertEquals("Onix", vehiculo.getModelo());
    }

    @Test
    @DisplayName("Debe generar una representación en texto legible en toString")
    void testToString() {
        Vehiculo vehiculo = new Vehiculo(1, 2, "ABC123", "Toyota", "Corolla");
        String texto = vehiculo.toString();

        assertNotNull(texto);
        assertTrue(texto.contains("ABC123"));
        assertTrue(texto.contains("Toyota"));
        assertTrue(texto.contains("Corolla"));
        assertTrue(texto.contains("Cliente ID: 2"));
    }
}
