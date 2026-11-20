package com.carmotors.sgt.model;

/**
 * Métricas de inventario de un repuesto (RF-07, sección 2.5). Solo transporta datos
 * ya calculados por {@code InventarioService}; no contiene lógica de negocio.
 */
public class StockRepuesto {

    private Repuesto repuesto;
    private int stockFisico;
    private int stockOperativo;
    private int reservado;
    private int disponible;
    private int deficit;
    private int stockMinimo;
    private String proveedorHabitual;

    public StockRepuesto() {
        this.proveedorHabitual = "No definido";
    }

    public StockRepuesto(Repuesto repuesto, int stockFisico, int stockOperativo, int reservado,
                         int disponible, int deficit, int stockMinimo) {
        this(repuesto, stockFisico, stockOperativo, reservado, disponible, deficit, stockMinimo, "No definido");
    }

    public StockRepuesto(Repuesto repuesto, int stockFisico, int stockOperativo, int reservado,
                         int disponible, int deficit, int stockMinimo, String proveedorHabitual) {
        this.repuesto = repuesto;
        this.stockFisico = stockFisico;
        this.stockOperativo = stockOperativo;
        this.reservado = reservado;
        this.disponible = disponible;
        this.deficit = deficit;
        this.stockMinimo = stockMinimo;
        this.proveedorHabitual = proveedorHabitual == null ? "No definido" : proveedorHabitual;
    }

    public Repuesto getRepuesto() {
        return repuesto;
    }

    public void setRepuesto(Repuesto repuesto) {
        this.repuesto = repuesto;
    }

    public int getStockFisico() {
        return stockFisico;
    }

    public void setStockFisico(int stockFisico) {
        this.stockFisico = stockFisico;
    }

    public int getStockOperativo() {
        return stockOperativo;
    }

    public void setStockOperativo(int stockOperativo) {
        this.stockOperativo = stockOperativo;
    }

    public int getReservado() {
        return reservado;
    }

    public void setReservado(int reservado) {
        this.reservado = reservado;
    }

    public int getDisponible() {
        return disponible;
    }

    public void setDisponible(int disponible) {
        this.disponible = disponible;
    }

    public int getDeficit() {
        return deficit;
    }

    public void setDeficit(int deficit) {
        this.deficit = deficit;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getProveedorHabitual() {
        return proveedorHabitual;
    }

    public void setProveedorHabitual(String proveedorHabitual) {
        this.proveedorHabitual = proveedorHabitual == null ? "No definido" : proveedorHabitual;
    }

    @Override
    public String toString() {
        return "StockRepuesto{" +
                "repuesto=" + (repuesto == null ? null : repuesto.getNombre()) +
                ", stockFisico=" + stockFisico +
                ", stockOperativo=" + stockOperativo +
                ", reservado=" + reservado +
                ", disponible=" + disponible +
                ", deficit=" + deficit +
                ", stockMinimo=" + stockMinimo +
                ", proveedorHabitual='" + proveedorHabitual + '\'' +
                '}';
    }
}
