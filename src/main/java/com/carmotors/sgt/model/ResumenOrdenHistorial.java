package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de una orden de servicio dentro del historial de un cliente (RF-20, HU-10).
 *
 * <p>El total facturado es {@code null} cuando la orden no tiene factura; nunca se calcula
 * ni se estima a partir del catálogo vigente.</p>
 */
public class ResumenOrdenHistorial {

    private int ordenId;
    private String estado;
    private LocalDateTime fechaRecepcion;
    private String placa;
    private List<OrdenServicioDetalle> lineas = new ArrayList<>();
    private BigDecimal totalFacturado;
    private String numeroFactura;

    public ResumenOrdenHistorial() {
    }

    public ResumenOrdenHistorial(int ordenId, String estado, LocalDateTime fechaRecepcion, String placa,
                                 List<OrdenServicioDetalle> lineas, BigDecimal totalFacturado,
                                 String numeroFactura) {
        this.ordenId = ordenId;
        this.estado = estado;
        this.fechaRecepcion = fechaRecepcion;
        this.placa = placa;
        this.lineas = lineas;
        this.totalFacturado = totalFacturado;
        this.numeroFactura = numeroFactura;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(int ordenId) {
        this.ordenId = ordenId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public List<OrdenServicioDetalle> getLineas() {
        return lineas;
    }

    public void setLineas(List<OrdenServicioDetalle> lineas) {
        this.lineas = lineas;
    }

    public BigDecimal getTotalFacturado() {
        return totalFacturado;
    }

    public void setTotalFacturado(BigDecimal totalFacturado) {
        this.totalFacturado = totalFacturado;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    /**
     * Indica si la orden tiene factura asociada.
     *
     * @return {@code true} si existe un total facturado registrado
     */
    public boolean isFacturada() {
        return totalFacturado != null;
    }

    @Override
    public String toString() {
        return "ResumenOrdenHistorial{" +
                "ordenId=" + ordenId +
                ", estado='" + estado + '\'' +
                ", fechaRecepcion=" + fechaRecepcion +
                ", placa='" + placa + '\'' +
                ", totalFacturado=" + totalFacturado +
                ", numeroFactura='" + numeroFactura + '\'' +
                '}';
    }
}
