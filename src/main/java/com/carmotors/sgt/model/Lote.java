package com.carmotors.sgt.model;

import java.time.LocalDate;

/**
 * Registro de ingreso de inventario por lote.
 *
 * <p>La cantidad inicial y la cantidad actual deben mantenerse sincronizadas al crear el lote,
 * ya que el stock físico se deriva de los lotes y no se duplica en el catálogo de repuestos.</p>
 */
public class Lote {

    private int id;
    private String codigoLote;
    private int proveedorId;
    private int repuestoId;
    private LocalDate fechaIngreso;
    private LocalDate fechaCaducidad;
    private int cantidadInicial;
    private int cantidadActual;

    public Lote() {
    }

    public Lote(int id, String codigoLote, int proveedorId, int repuestoId,
                LocalDate fechaIngreso, LocalDate fechaCaducidad,
                int cantidadInicial, int cantidadActual) {
        this.id = id;
        this.codigoLote = codigoLote;
        this.proveedorId = proveedorId;
        this.repuestoId = repuestoId;
        this.fechaIngreso = fechaIngreso;
        this.fechaCaducidad = fechaCaducidad;
        this.cantidadInicial = cantidadInicial;
        this.cantidadActual = cantidadActual;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public int getRepuestoId() {
        return repuestoId;
    }

    public void setRepuestoId(int repuestoId) {
        this.repuestoId = repuestoId;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public LocalDate getFechaCaducidad() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(LocalDate fechaCaducidad) {
        this.fechaCaducidad = fechaCaducidad;
    }

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(int cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public int getCantidadActual() {
        return cantidadActual;
    }

    public void setCantidadActual(int cantidadActual) {
        this.cantidadActual = cantidadActual;
    }

    @Override
    public String toString() {
        return "Lote{" +
                "id=" + id +
                ", codigoLote='" + codigoLote + '\'' +
                ", proveedorId=" + proveedorId +
                ", repuestoId=" + repuestoId +
                ", fechaIngreso=" + fechaIngreso +
                ", fechaCaducidad=" + fechaCaducidad +
                ", cantidadInicial=" + cantidadInicial +
                ", cantidadActual=" + cantidadActual +
                '}';
    }
}
