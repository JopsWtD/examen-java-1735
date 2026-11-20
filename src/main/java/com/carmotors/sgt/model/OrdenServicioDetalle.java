package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Línea de servicio de una orden (RF-08, RN-11, RN-18).
 * Mapea la tabla {@code orden_servicio_detalle}; el nombre del servicio y la mano de obra
 * quedan congelados al crear la línea.
 */
public class OrdenServicioDetalle {

    private int id;
    private int ordenId;
    private int servicioTipoId;
    private String nombreServicioSnapshot;
    private BigDecimal costoManoObra;
    private String observaciones;

    public OrdenServicioDetalle() {
    }

    public OrdenServicioDetalle(int servicioTipoId, String observaciones) {
        this.servicioTipoId = servicioTipoId;
        this.observaciones = observaciones;
    }

    public OrdenServicioDetalle(int id, int ordenId, int servicioTipoId, String nombreServicioSnapshot,
                                BigDecimal costoManoObra, String observaciones) {
        this.id = id;
        this.ordenId = ordenId;
        this.servicioTipoId = servicioTipoId;
        this.nombreServicioSnapshot = nombreServicioSnapshot;
        this.costoManoObra = costoManoObra;
        this.observaciones = observaciones;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(int ordenId) {
        this.ordenId = ordenId;
    }

    public int getServicioTipoId() {
        return servicioTipoId;
    }

    public void setServicioTipoId(int servicioTipoId) {
        this.servicioTipoId = servicioTipoId;
    }

    public String getNombreServicioSnapshot() {
        return nombreServicioSnapshot;
    }

    public void setNombreServicioSnapshot(String nombreServicioSnapshot) {
        this.nombreServicioSnapshot = nombreServicioSnapshot;
    }

    public BigDecimal getCostoManoObra() {
        return costoManoObra;
    }

    public void setCostoManoObra(BigDecimal costoManoObra) {
        this.costoManoObra = costoManoObra;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        String obs = observaciones == null ? "N/A" : observaciones;
        return "ID: " + id
                + " | Orden ID: " + ordenId
                + " | Servicio: " + nombreServicioSnapshot
                + " | Mano de obra: $" + costoManoObra
                + " | Observaciones: " + obs;
    }
}
