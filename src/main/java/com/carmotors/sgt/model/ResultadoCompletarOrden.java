package com.carmotors.sgt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de solo transporte con el resultado de completar una orden (RF-13):
 * la orden actualizada y las unidades descontadas por repuesto.
 */
public class ResultadoCompletarOrden {
    private OrdenServicio orden;
    private List<ConsumoRepuesto> consumos = new ArrayList<>();

    public ResultadoCompletarOrden() {
    }

    public ResultadoCompletarOrden(OrdenServicio orden, List<ConsumoRepuesto> consumos) {
        this.orden = orden;
        setConsumos(consumos);
    }

    public OrdenServicio getOrden() {
        return orden;
    }

    public void setOrden(OrdenServicio orden) {
        this.orden = orden;
    }

    public List<ConsumoRepuesto> getConsumos() {
        return consumos;
    }

    public void setConsumos(List<ConsumoRepuesto> consumos) {
        this.consumos = consumos != null ? new ArrayList<>(consumos) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ResultadoCompletarOrden{" +
                "orden=" + orden +
                ", consumos=" + consumos +
                '}';
    }
}
