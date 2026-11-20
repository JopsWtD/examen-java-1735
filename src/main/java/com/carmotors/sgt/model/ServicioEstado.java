package com.carmotors.sgt.model;

/**
 * Entidad que representa los estados del ciclo de vida de un servicio u orden
 * (ej. Pendiente, En Proceso, Completado, Facturado, Entregado, Cancelada).
 * Mapea la tabla de consulta {@code servicio_estados}.
 */
public class ServicioEstado {

    private int id;
    private String nombre;

    public ServicioEstado() {
    }

    public ServicioEstado(int id, String nombre) {
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
