package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Asociación M:N entre proveedor y repuesto (RF-29): qué repuesto ofrece un proveedor,
 * a qué costo, con qué tiempo de entrega y si es el proveedor habitual del repuesto.
 * La clave es compuesta (proveedorId, repuestoId), por eso no tiene campo id.
 */
public class ProveedorRepuesto {

    private int proveedorId;
    private int repuestoId;
    private BigDecimal costo;
    private int tiempoEntregaDias;
    private boolean esHabitual;

    public ProveedorRepuesto() {
    }

    public ProveedorRepuesto(int proveedorId, int repuestoId, BigDecimal costo,
                             int tiempoEntregaDias, boolean esHabitual) {
        this.proveedorId = proveedorId;
        this.repuestoId = repuestoId;
        this.costo = costo;
        this.tiempoEntregaDias = tiempoEntregaDias;
        this.esHabitual = esHabitual;
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public int getRepuestoId() {
        return repuestoId;
    }

    public void setRepuestoId(int repuestoId) {
        this.repuestoId = repuestoId;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public int getTiempoEntregaDias() {
        return tiempoEntregaDias;
    }

    public void setTiempoEntregaDias(int tiempoEntregaDias) {
        this.tiempoEntregaDias = tiempoEntregaDias;
    }

    public boolean isEsHabitual() {
        return esHabitual;
    }

    public void setEsHabitual(boolean esHabitual) {
        this.esHabitual = esHabitual;
    }

    @Override
    public String toString() {
        return "ProveedorRepuesto{" +
                "proveedorId=" + proveedorId +
                ", repuestoId=" + repuestoId +
                ", costo=" + costo +
                ", tiempoEntregaDias=" + tiempoEntregaDias +
                ", esHabitual=" + esHabitual +
                '}';
    }
}
