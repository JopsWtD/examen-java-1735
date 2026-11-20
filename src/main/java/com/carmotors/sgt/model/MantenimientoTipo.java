package com.carmotors.sgt.model;

/**
 * Entidad que representa la categoría de un servicio u orden de trabajo
 * (ej. Preventivo, Correctivo).
 * Mapea la tabla de consulta {@code mantenimiento_tipos}.
 */
public class MantenimientoTipo {

    private int id;
    private String nombre;

    public MantenimientoTipo() {
    }

    public MantenimientoTipo(int id, String nombre) {
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
