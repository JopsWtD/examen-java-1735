package com.carmotors.sgt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de solo transporte: una factura con su snapshot de cliente y vehículo y sus conceptos.
 */
public class ConsultaFactura {
    private Factura factura;
    private FacturaSnapshot snapshot;
    private List<FacturaDetalle> detalles = new ArrayList<>();

    public ConsultaFactura() {
    }

    public ConsultaFactura(Factura factura, FacturaSnapshot snapshot, List<FacturaDetalle> detalles) {
        this.factura = factura;
        this.snapshot = snapshot;
        setDetalles(detalles);
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

    public FacturaSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(FacturaSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public List<FacturaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<FacturaDetalle> detalles) {
        this.detalles = detalles != null ? new ArrayList<>(detalles) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ConsultaFactura{" +
                "factura=" + factura +
                ", snapshot=" + snapshot +
                ", detalles=" + detalles +
                '}';
    }
}
