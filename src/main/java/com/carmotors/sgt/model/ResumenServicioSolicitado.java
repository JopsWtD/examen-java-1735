package com.carmotors.sgt.model;

import java.math.BigDecimal;

/** Volumen e ingresos antes de impuestos por tipo de servicio. */
public class ResumenServicioSolicitado {
    private final int servicioTipoId;
    private final String nombreServicio;
    private final int cantidadLineas;
    private final BigDecimal ingresos;

    public ResumenServicioSolicitado(int servicioTipoId, String nombreServicio, int cantidadLineas, BigDecimal ingresos) {
        this.servicioTipoId = servicioTipoId;
        this.nombreServicio = nombreServicio;
        this.cantidadLineas = cantidadLineas;
        this.ingresos = ingresos;
    }

    public int getServicioTipoId() {
        return servicioTipoId;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public int getCantidadLineas() {
        return cantidadLineas;
    }

    public BigDecimal getIngresos() {
        return ingresos;
    }
}
