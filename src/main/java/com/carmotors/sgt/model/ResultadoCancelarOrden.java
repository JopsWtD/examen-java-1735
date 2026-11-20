package com.carmotors.sgt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de solo transporte con el resultado de cancelar una orden de servicio (RF-14, RN-05):
 * la orden actualizada a estado Cancelada y el detalle de reservas liberadas por repuesto.
 */
public class ResultadoCancelarOrden {
    private OrdenServicio orden;
    private List<ReservaLiberada> reservasLiberadas = new ArrayList<>();

    public ResultadoCancelarOrden() {
    }

    public ResultadoCancelarOrden(OrdenServicio orden, List<ReservaLiberada> reservasLiberadas) {
        this.orden = orden;
        setReservasLiberadas(reservasLiberadas);
    }

    public OrdenServicio getOrden() {
        return orden;
    }

    public void setOrden(OrdenServicio orden) {
        this.orden = orden;
    }

    public List<ReservaLiberada> getReservasLiberadas() {
        return reservasLiberadas;
    }

    public void setReservasLiberadas(List<ReservaLiberada> reservasLiberadas) {
        this.reservasLiberadas = reservasLiberadas != null
                ? new ArrayList<>(reservasLiberadas)
                : new ArrayList<>();
    }

    public int getTotalUnidadesLiberadas() {
        int total = 0;
        for (ReservaLiberada reserva : reservasLiberadas) {
            total += reserva.getUnidadesLiberadas();
        }
        return total;
    }

    @Override
    public String toString() {
        return "ResultadoCancelarOrden{" +
                "orden=" + orden +
                ", reservasLiberadas=" + reservasLiberadas +
                '}';
    }
}
