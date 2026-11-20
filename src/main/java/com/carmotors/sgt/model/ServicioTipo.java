package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Entidad que representa un tipo de servicio ofrecido en el catálogo del taller.
 * Mapea la tabla {@code servicio_tipos}.
 */
public class ServicioTipo {

    private int id;
    private String nombre;
    private int mantenimientoTipoId;
    private int especialidadId;
    private BigDecimal precioManoObraBase;
    private Integer intervaloMeses;

    public ServicioTipo() {
    }

    public ServicioTipo(
            int id,
            String nombre,
            int mantenimientoTipoId,
            int especialidadId,
            BigDecimal precioManoObraBase,
            Integer intervaloMeses) {
        this.id = id;
        this.nombre = nombre;
        this.mantenimientoTipoId = mantenimientoTipoId;
        this.especialidadId = especialidadId;
        this.precioManoObraBase = precioManoObraBase;
        this.intervaloMeses = intervaloMeses;
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

    public int getMantenimientoTipoId() {
        return mantenimientoTipoId;
    }

    public void setMantenimientoTipoId(int mantenimientoTipoId) {
        this.mantenimientoTipoId = mantenimientoTipoId;
    }

    public int getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(int especialidadId) {
        this.especialidadId = especialidadId;
    }

    public BigDecimal getPrecioManoObraBase() {
        return precioManoObraBase;
    }

    public void setPrecioManoObraBase(BigDecimal precioManoObraBase) {
        this.precioManoObraBase = precioManoObraBase;
    }

    public Integer getIntervaloMeses() {
        return intervaloMeses;
    }

    public void setIntervaloMeses(Integer intervaloMeses) {
        this.intervaloMeses = intervaloMeses;
    }

    @Override
    public String toString() {
        String intervalo = intervaloMeses == null ? "N/A" : intervaloMeses + " meses";
        return "ID: " + id
                + " | Nombre: " + nombre
                + " | Mantenimiento ID: " + mantenimientoTipoId
                + " | Especialidad ID: " + especialidadId
                + " | Mano de obra base: $" + precioManoObraBase
                + " | Intervalo: " + intervalo;
    }
}
