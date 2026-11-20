package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * DTO de transporte para la comparación de proveedores de un repuesto (RF-30, RN-16, RNF-11).
 * Contiene información financiera, logística y calificación histórica.
 */
public class ComparacionProveedor {

    private int proveedorId;
    private String nombreProveedor;
    private BigDecimal costo;
    private int tiempoEntregaDias;
    private boolean esHabitual;
    private BigDecimal promedioEvaluacion;
    private int cantidadEvaluaciones;

    public ComparacionProveedor() {
    }

    public ComparacionProveedor(int proveedorId, String nombreProveedor, BigDecimal costo,
                                int tiempoEntregaDias, boolean esHabitual,
                                BigDecimal promedioEvaluacion, int cantidadEvaluaciones) {
        this.proveedorId = proveedorId;
        this.nombreProveedor = nombreProveedor;
        this.costo = costo;
        this.tiempoEntregaDias = tiempoEntregaDias;
        this.esHabitual = esHabitual;
        this.promedioEvaluacion = promedioEvaluacion;
        this.cantidadEvaluaciones = cantidadEvaluaciones;
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
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

    public BigDecimal getPromedioEvaluacion() {
        return promedioEvaluacion;
    }

    public void setPromedioEvaluacion(BigDecimal promedioEvaluacion) {
        this.promedioEvaluacion = promedioEvaluacion;
    }

    public int getCantidadEvaluaciones() {
        return cantidadEvaluaciones;
    }

    public void setCantidadEvaluaciones(int cantidadEvaluaciones) {
        this.cantidadEvaluaciones = cantidadEvaluaciones;
    }

    @Override
    public String toString() {
        String calificacion = (promedioEvaluacion != null)
                ? promedioEvaluacion.toString() + " (" + cantidadEvaluaciones + " eval)"
                : "Sin evaluar";
        return "Proveedor: " + nombreProveedor
                + " (ID " + proveedorId + ")"
                + " | Costo: $" + costo
                + " | Entrega: " + tiempoEntregaDias + " días"
                + " | Habitual: " + (esHabitual ? "Sí" : "No")
                + " | Calificación: " + calificacion;
    }
}
