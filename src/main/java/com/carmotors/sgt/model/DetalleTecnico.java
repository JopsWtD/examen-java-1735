package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Asignación de un técnico a una línea de servicio con sus horas trabajadas ({@code detalle_tecnicos}).
 * Mapea la clase de asociación del modelo v1.3 (RF-09, RN-07, RN-19).
 */
public class DetalleTecnico {

    private int detalleId;
    private int tecnicoId;
    private BigDecimal horasTrabajadas;

    public DetalleTecnico() {
    }

    public DetalleTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
        this.detalleId = detalleId;
        this.tecnicoId = tecnicoId;
        this.horasTrabajadas = horasTrabajadas;
    }

    public int getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(int detalleId) {
        this.detalleId = detalleId;
    }

    public int getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(int tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public BigDecimal getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(BigDecimal horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    @Override
    public String toString() {
        return "DetalleTecnico{" +
                "detalleId=" + detalleId +
                ", tecnicoId=" + tecnicoId +
                ", horasTrabajadas=" + horasTrabajadas +
                '}';
    }
}
