package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Resultado del reporte de productividad para un técnico (RF-26): líneas completadas en las que
 * participa, horas trabajadas y mano de obra atribuida en el periodo.
 */
public class ProductividadTecnico {
    private int tecnicoId;
    private String nombre;
    private boolean activo;
    private int lineasCompletadas;
    private BigDecimal horasTrabajadas;
    private BigDecimal manoObraAtribuida;

    public ProductividadTecnico() {
    }

    public ProductividadTecnico(int tecnicoId, String nombre, boolean activo, int lineasCompletadas,
                                BigDecimal horasTrabajadas, BigDecimal manoObraAtribuida) {
        this.tecnicoId = tecnicoId;
        this.nombre = nombre;
        this.activo = activo;
        this.lineasCompletadas = lineasCompletadas;
        this.horasTrabajadas = horasTrabajadas;
        this.manoObraAtribuida = manoObraAtribuida;
    }

    public int getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(int tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getLineasCompletadas() {
        return lineasCompletadas;
    }

    public void setLineasCompletadas(int lineasCompletadas) {
        this.lineasCompletadas = lineasCompletadas;
    }

    public BigDecimal getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(BigDecimal horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public BigDecimal getManoObraAtribuida() {
        return manoObraAtribuida;
    }

    public void setManoObraAtribuida(BigDecimal manoObraAtribuida) {
        this.manoObraAtribuida = manoObraAtribuida;
    }

    @Override
    public String toString() {
        return "ProductividadTecnico{" +
                "tecnicoId=" + tecnicoId +
                ", nombre='" + nombre + '\'' +
                ", activo=" + activo +
                ", lineasCompletadas=" + lineasCompletadas +
                ", horasTrabajadas=" + horasTrabajadas +
                ", manoObraAtribuida=" + manoObraAtribuida +
                '}';
    }
}
