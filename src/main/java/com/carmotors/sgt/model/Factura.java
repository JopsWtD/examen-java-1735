package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cabecera de una factura interna ({@code facturas}, sección 2.7).
 * Montos en {@code BigDecimal} con escala 2 y {@code HALF_UP} (RN-12, RNF-15).
 */
public class Factura {
    private int id;
    private String numeroFactura;
    private int ordenId;
    private LocalDateTime fechaEmision;
    private BigDecimal totalRepuestos;
    private BigDecimal totalManoObra;
    private BigDecimal subtotal;
    private BigDecimal tasaIva;
    private BigDecimal impuestos;
    private BigDecimal totalFinal;
    private boolean archivoGenerado;

    public Factura() {
    }

    public Factura(int id, String numeroFactura, int ordenId, LocalDateTime fechaEmision,
                   BigDecimal totalRepuestos, BigDecimal totalManoObra, BigDecimal subtotal, BigDecimal tasaIva,
                   BigDecimal impuestos, BigDecimal totalFinal, boolean archivoGenerado) {
        this.id = id;
        this.numeroFactura = numeroFactura;
        this.ordenId = ordenId;
        this.fechaEmision = fechaEmision;
        this.totalRepuestos = totalRepuestos;
        this.totalManoObra = totalManoObra;
        this.subtotal = subtotal;
        this.tasaIva = tasaIva;
        this.impuestos = impuestos;
        this.totalFinal = totalFinal;
        this.archivoGenerado = archivoGenerado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(int ordenId) {
        this.ordenId = ordenId;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public BigDecimal getTotalRepuestos() {
        return totalRepuestos;
    }

    public void setTotalRepuestos(BigDecimal totalRepuestos) {
        this.totalRepuestos = totalRepuestos;
    }

    public BigDecimal getTotalManoObra() {
        return totalManoObra;
    }

    public void setTotalManoObra(BigDecimal totalManoObra) {
        this.totalManoObra = totalManoObra;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTasaIva() {
        return tasaIva;
    }

    public void setTasaIva(BigDecimal tasaIva) {
        this.tasaIva = tasaIva;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(BigDecimal impuestos) {
        this.impuestos = impuestos;
    }

    public BigDecimal getTotalFinal() {
        return totalFinal;
    }

    public void setTotalFinal(BigDecimal totalFinal) {
        this.totalFinal = totalFinal;
    }

    public boolean isArchivoGenerado() {
        return archivoGenerado;
    }

    public void setArchivoGenerado(boolean archivoGenerado) {
        this.archivoGenerado = archivoGenerado;
    }

    @Override
    public String toString() {
        return "Factura{" +
                "id=" + id +
                ", numeroFactura='" + numeroFactura + '\'' +
                ", ordenId=" + ordenId +
                ", fechaEmision=" + fechaEmision +
                ", totalRepuestos=" + totalRepuestos +
                ", totalManoObra=" + totalManoObra +
                ", subtotal=" + subtotal +
                ", tasaIva=" + tasaIva +
                ", impuestos=" + impuestos +
                ", totalFinal=" + totalFinal +
                ", archivoGenerado=" + archivoGenerado +
                '}';
    }
}
