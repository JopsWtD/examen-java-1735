package com.carmotors.sgt.model;

/**
 * Entidad que representa a un técnico mecánico del taller (RF-33, RN-17).
 * Mapea la tabla {@code tecnicos}.
 */
public class Tecnico {

    private int id;
    private String nombre;
    private String documentoIdentidad;
    private int especialidadId;
    private String telefono;
    private boolean activo = true;

    public Tecnico() {
    }

    public Tecnico(int id, String nombre, String documentoIdentidad, int especialidadId, String telefono, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.documentoIdentidad = documentoIdentidad;
        this.especialidadId = especialidadId;
        this.telefono = telefono;
        this.activo = activo;
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

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public int getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(int especialidadId) {
        this.especialidadId = especialidadId;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        String estado = activo ? "Activo" : "Inactivo";
        String tel = (telefono == null || telefono.isEmpty()) ? "Sin teléfono" : telefono;
        return "ID: " + id
                + " | Nombre: " + nombre
                + " | Doc: " + documentoIdentidad
                + " | Especialidad ID: " + especialidadId
                + " | Tel: " + tel
                + " | Estado: " + estado;
    }
}
