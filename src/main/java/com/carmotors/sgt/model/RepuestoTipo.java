package com.carmotors.sgt.model;

/**
 * Entidad que representa la clasificación o tipo de un repuesto en el inventario
 * (ej. Mecánico, Eléctrico, Carrocería, Consumo).
 * Mapea la tabla de consulta {@code repuesto_tipos}.
 */
public class RepuestoTipo {

    private int id;
    private String nombre;

    public RepuestoTipo() {
    }

    public RepuestoTipo(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return id + " - " + nombre;
    }
}
