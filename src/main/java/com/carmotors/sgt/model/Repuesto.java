package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * Entidad de dominio para un repuesto del inventario.
 *
 * <p>El stock físico no se almacena ni se actualiza directamente aquí, sino que
 * se deriva de los lotes asociados a cada repuesto. La cantidad reservada se
 * controla por las órdenes en servicio y no debe editarse desde la UI de CRUD.</p>
 */
public class Repuesto {

    private int id;
    private int repuestoTipoId;
    private String nombre;
    private String marca;
    private BigDecimal precioVenta;
    private int cantidadReservada;
    private int stockMinimo;
    private boolean activo = true;

    public Repuesto() {
    }

    public Repuesto(int id, int repuestoTipoId, String nombre, String marca,
                    BigDecimal precioVenta, int cantidadReservada, int stockMinimo, boolean activo) {
        this.id = id;
        this.repuestoTipoId = repuestoTipoId;
        this.nombre = nombre;
        this.marca = marca;
        this.precioVenta = precioVenta;
        this.cantidadReservada = cantidadReservada;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRepuestoTipoId() {
        return repuestoTipoId;
    }

    public void setRepuestoTipoId(int repuestoTipoId) {
        this.repuestoTipoId = repuestoTipoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getCantidadReservada() {
        return cantidadReservada;
    }

    public void setCantidadReservada(int cantidadReservada) {
        this.cantidadReservada = cantidadReservada;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Repuesto{" +
                "id=" + id +
                ", repuestoTipoId=" + repuestoTipoId +
                ", nombre='" + nombre + '\'' +
                ", marca='" + marca + '\'' +
                ", precioVenta=" + precioVenta +
                ", cantidadReservada=" + cantidadReservada +
                ", stockMinimo=" + stockMinimo +
                ", activo=" + activo +
                '}';
    }
}
