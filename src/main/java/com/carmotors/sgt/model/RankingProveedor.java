package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Fila del ranking de proveedores (RF-32). Solo transporta datos ya calculados
 * por el servicio. Un promedio {@code null} indica un proveedor sin evaluaciones.
 */
public class RankingProveedor {

    private Proveedor proveedor;
    private BigDecimal promedio;
    private int cantidadEvaluaciones;

    public RankingProveedor() {
    }

    public RankingProveedor(Proveedor proveedor, BigDecimal promedio, int cantidadEvaluaciones) {
        this.proveedor = proveedor;
        this.promedio = promedio;
        this.cantidadEvaluaciones = cantidadEvaluaciones;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public BigDecimal getPromedio() {
        return promedio;
    }

    public void setPromedio(BigDecimal promedio) {
        this.promedio = promedio;
    }

    public int getCantidadEvaluaciones() {
        return cantidadEvaluaciones;
    }

    public void setCantidadEvaluaciones(int cantidadEvaluaciones) {
        this.cantidadEvaluaciones = cantidadEvaluaciones;
    }

    @Override
    public String toString() {
        return "RankingProveedor{" +
                "proveedor=" + (proveedor == null ? null : proveedor.getNombre()) +
                ", promedio=" + promedio +
                ", cantidadEvaluaciones=" + cantidadEvaluaciones +
                '}';
    }
}
