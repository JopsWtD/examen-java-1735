package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de solo transporte con el reporte de productividad por técnico (RF-26, §4.3): resultado por
 * técnico, mano de obra "sin distribuir" (líneas sin horas) y mano de obra total del periodo.
 */
public class ReporteProductividadTecnicos {
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private List<ProductividadTecnico> tecnicos = new ArrayList<>();
    private int lineasCompletadas;
    private BigDecimal manoObraSinDistribuir = BigDecimal.ZERO;
    private BigDecimal manoObraTotal = BigDecimal.ZERO;

    public ReporteProductividadTecnicos() {
    }

    public ReporteProductividadTecnicos(LocalDate fechaDesde, LocalDate fechaHasta, List<ProductividadTecnico> tecnicos,
                                        int lineasCompletadas, BigDecimal manoObraSinDistribuir,
                                        BigDecimal manoObraTotal) {
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        setTecnicos(tecnicos);
        this.lineasCompletadas = lineasCompletadas;
        this.manoObraSinDistribuir = manoObraSinDistribuir != null ? manoObraSinDistribuir : BigDecimal.ZERO;
        this.manoObraTotal = manoObraTotal != null ? manoObraTotal : BigDecimal.ZERO;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public List<ProductividadTecnico> getTecnicos() {
        return tecnicos;
    }

    public void setTecnicos(List<ProductividadTecnico> tecnicos) {
        this.tecnicos = tecnicos != null ? new ArrayList<>(tecnicos) : new ArrayList<>();
    }

    /** Número de líneas de órdenes completadas o posteriores en el periodo. */
    public int getLineasCompletadas() {
        return lineasCompletadas;
    }

    public void setLineasCompletadas(int lineasCompletadas) {
        this.lineasCompletadas = lineasCompletadas;
    }

    public BigDecimal getManoObraSinDistribuir() {
        return manoObraSinDistribuir;
    }

    public void setManoObraSinDistribuir(BigDecimal manoObraSinDistribuir) {
        this.manoObraSinDistribuir = manoObraSinDistribuir;
    }

    /** Suma de la mano de obra de todas las líneas del periodo (atribuida + sin distribuir). */
    public BigDecimal getManoObraTotal() {
        return manoObraTotal;
    }

    public void setManoObraTotal(BigDecimal manoObraTotal) {
        this.manoObraTotal = manoObraTotal;
    }

    /** Indica si no hubo líneas de órdenes completadas en el periodo. */
    public boolean sinLineasEnPeriodo() {
        return lineasCompletadas == 0;
    }

    @Override
    public String toString() {
        return "ReporteProductividadTecnicos{" +
                "fechaDesde=" + fechaDesde +
                ", fechaHasta=" + fechaHasta +
                ", tecnicos=" + tecnicos +
                ", lineasCompletadas=" + lineasCompletadas +
                ", manoObraSinDistribuir=" + manoObraSinDistribuir +
                ", manoObraTotal=" + manoObraTotal +
                '}';
    }
}
