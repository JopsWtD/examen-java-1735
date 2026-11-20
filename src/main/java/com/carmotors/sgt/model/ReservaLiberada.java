package com.carmotors.sgt.model;

/**
 * DTO de solo transporte: unidades reservadas de un repuesto que se liberaron
 * al cancelar una orden de servicio (RF-14, RN-05).
 */
public class ReservaLiberada {
    private int repuestoId;
    private String nombreRepuesto;
    private int unidadesLiberadas;

    public ReservaLiberada() {
    }

    public ReservaLiberada(int repuestoId, String nombreRepuesto, int unidadesLiberadas) {
        this.repuestoId = repuestoId;
        this.nombreRepuesto = nombreRepuesto;
        this.unidadesLiberadas = unidadesLiberadas;
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

    public int getUnidadesLiberadas() {
        return unidadesLiberadas;
    }

    public void setUnidadesLiberadas(int unidadesLiberadas) {
        this.unidadesLiberadas = unidadesLiberadas;
    }

    @Override
    public String toString() {
        return "ReservaLiberada{" +
                "repuestoId=" + repuestoId +
                ", nombreRepuesto='" + nombreRepuesto + '\'' +
                ", unidadesLiberadas=" + unidadesLiberadas +
                '}';
    }
}
