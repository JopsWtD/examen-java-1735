package com.carmotors.sgt.model;

/**
 * Entidad que representa a un cliente del taller (RF-17, RN-13, RN-17).
 * Mapea la tabla {@code clientes}.
 */
public class Cliente {

    private int id;
    private String nombre;
    private String documentoIdentidad;
    private String telefono;
    private String email;
    private boolean activo = true;

    public Cliente() {
    }

    public Cliente(int id, String nombre, String documentoIdentidad, String telefono, String email, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.documentoIdentidad = documentoIdentidad;
        this.telefono = telefono;
        this.email = email;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
                + " | Tel: " + tel
                + " | Email: " + email
                + " | Estado: " + estado;
    }
}
