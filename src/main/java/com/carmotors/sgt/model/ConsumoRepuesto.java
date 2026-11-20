package com.carmotors.sgt.model;

/**
 * DTO de solo transporte: unidades de un repuesto descontadas del inventario
 * al completar una orden (RF-13).
 */
public class ConsumoRepuesto {
    private int repuestoId;
    private String nombreRepuesto;
    private int unidadesDescontadas;

    public ConsumoRepuesto() {
    }

    public ConsumoRepuesto(int repuestoId, String nombreRepuesto, int unidadesDescontadas) {
        this.repuestoId = repuestoId;
        this.nombreRepuesto = nombreRepuesto;
        this.unidadesDescontadas = unidadesDescontadas;
    }

    public int getRepuestoId() {
        return repuestoId;
    }

    public void setRepuestoId(int repuestoId) {
        this.repuestoId = repuestoId;
    }

    public String getNombreRepuesto() {
        return nombreRepuesto;
    }

    public void setNombreRepuesto(String nombreRepuesto) {
        this.nombreRepuesto = nombreRepuesto;
    }

    public int getUnidadesDescontadas() {
        return unidadesDescontadas;
    }

    public void setUnidadesDescontadas(int unidadesDescontadas) {
        this.unidadesDescontadas = unidadesDescontadas;
    }

    @Override
    public String toString() {
        return "ConsumoRepuesto{" +
                "repuestoId=" + repuestoId +
                ", nombreRepuesto='" + nombreRepuesto + '\'' +
                ", unidadesDescontadas=" + unidadesDescontadas +
                '}';
    }
}
