package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Registro plano para el reporte de productividad por técnico (RF-26): una fila por cada técnico
 * asignado a una línea de una orden completada o posterior. Si la línea no tiene técnicos, la fila
 * llega con {@code tecnicoId} nulo para poder sumar su mano de obra como "sin distribuir".
 */
public class RegistroProductividadTecnico {
    private final int detalleId;
    private final BigDecimal costoManoObra;
    private final Integer tecnicoId;
    private final String nombreTecnico;
    private final boolean tecnicoActivo;
    private final BigDecimal horasTrabajadas;

    public RegistroProductividadTecnico(int detalleId, BigDecimal costoManoObra, Integer tecnicoId,
                                        String nombreTecnico, boolean tecnicoActivo, BigDecimal horasTrabajadas) {
        this.detalleId = detalleId;
        this.costoManoObra = costoManoObra;
        this.tecnicoId = tecnicoId;
        this.nombreTecnico = nombreTecnico;
        this.tecnicoActivo = tecnicoActivo;
        this.horasTrabajadas = horasTrabajadas;
    }

    public int getDetalleId() {
        return detalleId;
    }

    public BigDecimal getCostoManoObra() {
        return costoManoObra;
    }

    public Integer getTecnicoId() {
        return tecnicoId;
    }

    public String getNombreTecnico() {
        return nombreTecnico;
    }

    public boolean isTecnicoActivo() {
        return tecnicoActivo;
    }

    public BigDecimal getHorasTrabajadas() {
        return horasTrabajadas;
    }

    /** Indica si la fila corresponde a un técnico asignado (y no a una línea sin técnicos). */
    public boolean tieneTecnico() {
        return tecnicoId != null;
    }

    @Override
    public String toString() {
        return "RegistroProductividadTecnico{" +
                "detalleId=" + detalleId +
                ", costoManoObra=" + costoManoObra +
                ", tecnicoId=" + tecnicoId +
                ", nombreTecnico='" + nombreTecnico + '\'' +
                ", tecnicoActivo=" + tecnicoActivo +
                ", horasTrabajadas=" + horasTrabajadas +
                '}';
    }
}
