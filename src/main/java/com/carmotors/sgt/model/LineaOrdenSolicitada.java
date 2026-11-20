package com.carmotors.sgt.model;

import java.util.List;

/** Una línea nueva y los repuestos solicitados para esa línea. */
public class LineaOrdenSolicitada {
    private final OrdenServicioDetalle linea;
    private final List<DetalleRepuesto> reservas;

    public LineaOrdenSolicitada(OrdenServicioDetalle linea, List<DetalleRepuesto> reservas) {
        this.linea = linea;
        this.reservas = reservas;
    }

    public OrdenServicioDetalle getLinea() {
        return linea;
    }

    public List<DetalleRepuesto> getReservas() {
        return reservas;
    }
}
