package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Concepto cobrado en una factura ({@code factura_detalles}, sección 2.7).
 * {@code repuestoId} es nulo para {@code MANO_OBRA} y obligatorio para {@code REPUESTO}.
 */
public class FacturaDetalle {
    private int id;
    private int facturaId;
    private int ordenDetalleId;
    private Integer repuestoId;
    private TipoConcepto tipoConcepto;
    private String descripcionSnapshot;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public FacturaDetalle() {
    }

    public FacturaDetalle(int id, int facturaId, int ordenDetalleId, Integer repuestoId,
                          TipoConcepto tipoConcepto, String descripcionSnapshot, int cantidad,
                          BigDecimal precioUnitario, BigDecimal subtotal) {
        this.id = id;
        this.facturaId = facturaId;
        this.ordenDetalleId = ordenDetalleId;
        this.repuestoId = repuestoId;
        this.tipoConcepto = tipoConcepto;
        this.descripcionSnapshot = descripcionSnapshot;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFacturaId() {
        return facturaId;
    }

    public void setFacturaId(int facturaId) {
        this.facturaId = facturaId;
    }

    public int getOrdenDetalleId() {
        return ordenDetalleId;
    }

    public void setOrdenDetalleId(int ordenDetalleId) {
        this.ordenDetalleId = ordenDetalleId;
    }

    public Integer getRepuestoId() {
        return repuestoId;
    }

    public void setRepuestoId(Integer repuestoId) {
        this.repuestoId = repuestoId;
    }

    public TipoConcepto getTipoConcepto() {
        return tipoConcepto;
    }

    public void setTipoConcepto(TipoConcepto tipoConcepto) {
        this.tipoConcepto = tipoConcepto;
    }

    public String getDescripcionSnapshot() {
        return descripcionSnapshot;
    }

    public void setDescripcionSnapshot(String descripcionSnapshot) {
        this.descripcionSnapshot = descripcionSnapshot;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public String toString() {
        return "FacturaDetalle{" +
                "id=" + id +
                ", facturaId=" + facturaId +
                ", ordenDetalleId=" + ordenDetalleId +
                ", repuestoId=" + repuestoId +
                ", tipoConcepto=" + tipoConcepto +
                ", descripcionSnapshot='" + descripcionSnapshot + '\'' +
                ", cantidad=" + cantidad +
                ", precioUnitario=" + precioUnitario +
                ", subtotal=" + subtotal +
                '}';
    }
}
