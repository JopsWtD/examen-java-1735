package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Repuesto reservado en una línea de servicio ({@code detalle_repuestos}).
 * Clave compuesta (detalleId, repuestoId); el nombre y el precio de venta quedan
 * congelados como snapshot al asignar (RN-11).
 */
public class DetalleRepuesto {
    private int detalleId;
    private int repuestoId;
    private int cantidadAsignada;
    private String nombreRepuestoSnapshot;
    private BigDecimal precioUnitarioVenta;

    public DetalleRepuesto() {
    }

    public DetalleRepuesto(int detalleId, int repuestoId, int cantidadAsignada, String nombreRepuestoSnapshot,
                           BigDecimal precioUnitarioVenta) {
        this.detalleId = detalleId;
        this.repuestoId = repuestoId;
        this.cantidadAsignada = cantidadAsignada;
        this.nombreRepuestoSnapshot = nombreRepuestoSnapshot;
        this.precioUnitarioVenta = precioUnitarioVenta;
    }

    public int getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(int detalleId) {
        this.detalleId = detalleId;
    }

    public int getRepuestoId() {
        return repuestoId;
    }

    public void setRepuestoId(int repuestoId) {
        this.repuestoId = repuestoId;
    }

    public int getCantidadAsignada() {
        return cantidadAsignada;
    }

    public void setCantidadAsignada(int cantidadAsignada) {
        this.cantidadAsignada = cantidadAsignada;
    }

    public String getNombreRepuestoSnapshot() {
        return nombreRepuestoSnapshot;
    }

    public void setNombreRepuestoSnapshot(String nombreRepuestoSnapshot) {
        this.nombreRepuestoSnapshot = nombreRepuestoSnapshot;
    }

    public BigDecimal getPrecioUnitarioVenta() {
        return precioUnitarioVenta;
    }

    public void setPrecioUnitarioVenta(BigDecimal precioUnitarioVenta) {
        this.precioUnitarioVenta = precioUnitarioVenta;
    }

    @Override
    public String toString() {
        return "DetalleRepuesto{" +
                "detalleId=" + detalleId +
                ", repuestoId=" + repuestoId +
                ", cantidadAsignada=" + cantidadAsignada +
                ", nombreRepuestoSnapshot='" + nombreRepuestoSnapshot + '\'' +
                ", precioUnitarioVenta=" + precioUnitarioVenta +
                '}';
    }
}
