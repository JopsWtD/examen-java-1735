package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila de una línea con un repuesto histórico, si lo tiene. */
public class RegistroServicioSolicitado {
    private final int detalleId;
    private final int servicioTipoId;
    private final String nombreServicio;
    private final int estadoId;
    private final LocalDateTime fechaCompletado;
    private final BigDecimal manoDeObra;
    private final int cantidadRepuesto;
    private final BigDecimal precioUnitarioVenta;

    public RegistroServicioSolicitado(int detalleId, int servicioTipoId, String nombreServicio, int estadoId,
                                      LocalDateTime fechaCompletado, BigDecimal manoDeObra, int cantidadRepuesto, BigDecimal precioUnitarioVenta) {
        this.detalleId = detalleId;
        this.servicioTipoId = servicioTipoId;
        this.nombreServicio = nombreServicio;
        this.estadoId = estadoId;
        this.fechaCompletado = fechaCompletado;
        this.manoDeObra = manoDeObra;
        this.cantidadRepuesto = cantidadRepuesto;
        this.precioUnitarioVenta = precioUnitarioVenta;
    }

    public int getDetalleId() {
        return detalleId;
    }

    public int getServicioTipoId() {
        return servicioTipoId;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public int getEstadoId() {
        return estadoId;
    }

    public LocalDateTime getFechaCompletado() {
        return fechaCompletado;
    }

    public BigDecimal getManoDeObra() {
        return manoDeObra;
    }

    public int getCantidadRepuesto() {
        return cantidadRepuesto;
    }

    public BigDecimal getPrecioUnitarioVenta() {
        return precioUnitarioVenta;
    }
}
