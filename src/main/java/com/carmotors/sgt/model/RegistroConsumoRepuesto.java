package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro histórico de consumo de un repuesto en una línea de orden de servicio (RF-27).
 * Conserva el nombre y precio unitario de venta congelados en la línea (RN-11) junto con el
 * estado y la fecha de finalización de la orden para su procesamiento con Stream API.
 */
public class RegistroConsumoRepuesto {
    private int ordenId;
    private int estadoId;
    private LocalDateTime fechaCompletado;
    private int repuestoId;
    private String nombreRepuesto;
    private int servicioTipoId;
    private String nombreServicio;
    private int cantidadAsignada;
    private BigDecimal precioUnitarioVenta;

    public RegistroConsumoRepuesto() {
    }

    public RegistroConsumoRepuesto(int ordenId, int estadoId, LocalDateTime fechaCompletado,
                                   int repuestoId, String nombreRepuesto,
                                   int servicioTipoId, String nombreServicio,
                                   int cantidadAsignada, BigDecimal precioUnitarioVenta) {
        this.ordenId = ordenId;
        this.estadoId = estadoId;
        this.fechaCompletado = fechaCompletado;
        this.repuestoId = repuestoId;
        this.nombreRepuesto = nombreRepuesto;
        this.servicioTipoId = servicioTipoId;
        this.nombreServicio = nombreServicio;
        this.cantidadAsignada = cantidadAsignada;
        this.precioUnitarioVenta = precioUnitarioVenta;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(int ordenId) {
        this.ordenId = ordenId;
    }

    public int getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(int estadoId) {
        this.estadoId = estadoId;
    }

    public LocalDateTime getFechaCompletado() {
        return fechaCompletado;
    }

    public void setFechaCompletado(LocalDateTime fechaCompletado) {
        this.fechaCompletado = fechaCompletado;
    }

    public int getRepuestoId() {
        return repuestoId;
    }

    public void setRepuestoId(int repuestoId) {
        this.repuestoId = repuestoId;
    }

    public String getNombreRepuesto() {
        return nombreRepuesto;
    }

    public void setNombreRepuesto(String nombreRepuesto) {
        this.nombreRepuesto = nombreRepuesto;
    }

    public int getServicioTipoId() {
        return servicioTipoId;
    }

    public void setServicioTipoId(int servicioTipoId) {
        this.servicioTipoId = servicioTipoId;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public int getCantidadAsignada() {
        return cantidadAsignada;
    }

    public void setCantidadAsignada(int cantidadAsignada) {
        this.cantidadAsignada = cantidadAsignada;
    }

    public BigDecimal getPrecioUnitarioVenta() {
        return precioUnitarioVenta;
    }

    public void setPrecioUnitarioVenta(BigDecimal precioUnitarioVenta) {
        this.precioUnitarioVenta = precioUnitarioVenta;
    }

    @Override
    public String toString() {
        return "RegistroConsumoRepuesto{" +
                "ordenId=" + ordenId +
                ", estadoId=" + estadoId +
                ", fechaCompletado=" + fechaCompletado +
                ", repuestoId=" + repuestoId +
                ", nombreRepuesto='" + nombreRepuesto + '\'' +
                ", servicioTipoId=" + servicioTipoId +
                ", nombreServicio='" + nombreServicio + '\'' +
                ", cantidadAsignada=" + cantidadAsignada +
                ", precioUnitarioVenta=" + precioUnitarioVenta +
                '}';
    }
}
