package com.carmotors.sgt.model;

/**
 * DTO de transporte para el reporte de lotes por vencer o ya vencidos (RF-06, HU-03, RNF-11).
 * Contiene los datos del lote, los nombres del repuesto y proveedor asociados,
 * su clasificación de caducidad y los días restantes hasta o desde su vencimiento.
 */
public class LotePorVencer {

    private Lote lote;
    private String nombreRepuesto;
    private String nombreProveedor;
    private String estado;
    private long diasRestantes;

    public LotePorVencer() {
    }

    public LotePorVencer(Lote lote, String nombreRepuesto, String nombreProveedor, String estado, long diasRestantes) {
        this.lote = lote;
        this.nombreRepuesto = nombreRepuesto;
        this.nombreProveedor = nombreProveedor;
        this.estado = estado;
        this.diasRestantes = diasRestantes;
    }

    public Lote getLote() {
        return lote;
    }

    public void setLote(Lote lote) {
        this.lote = lote;
    }

    public String getNombreRepuesto() {
        return nombreRepuesto;
    }

    public void setNombreRepuesto(String nombreRepuesto) {
        this.nombreRepuesto = nombreRepuesto;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(long diasRestantes) {
        this.diasRestantes = diasRestantes;
    }

    @Override
    public String toString() {
        return "LotePorVencer{" +
                "lote=" + (lote != null ? lote.getCodigoLote() : "null") +
                ", nombreRepuesto='" + nombreRepuesto + '\'' +
                ", nombreProveedor='" + nombreProveedor + '\'' +
                ", estado='" + estado + '\'' +
                ", diasRestantes=" + diasRestantes +
                '}';
    }
}
