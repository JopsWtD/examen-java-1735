package com.carmotors.sgt.model;

/**
 * Entidad que representa la especialidad técnica de un mecánico del taller
 * (ej. Mecánica general, Motor, Frenos, Eléctrica, Suspensión y dirección, Carrocería).
 * Mapea la tabla de consulta {@code especialidades}.
 */
public class Especialidad {

    private int id;
    private String nombre;

    public Especialidad() {
    }

    public Especialidad(int id, String nombre) {
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
