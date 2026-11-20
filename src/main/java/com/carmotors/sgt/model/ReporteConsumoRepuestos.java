package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado agregado del reporte de consumo de repuestos (RF-27, §4.2, §4.3).
 * Incluye el consumo totalizado por repuesto, por tipo de servicio y el desglose combinado
 * por repuesto y tipo de servicio, ordenados por valor comercial descendente.
 */
public class ReporteConsumoRepuestos {
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private List<ResumenConsumo> porRepuesto = new ArrayList<>();
    private List<ResumenConsumo> porTipoServicio = new ArrayList<>();
    private List<DetalleConsumo> detallePorRepuestoYServicio = new ArrayList<>();
    private int totalUnidades;
    private BigDecimal valorComercialTotal = BigDecimal.ZERO;

    public ReporteConsumoRepuestos() {
    }

    public ReporteConsumoRepuestos(LocalDate fechaDesde, LocalDate fechaHasta,
                                   List<ResumenConsumo> porRepuesto,
                                   List<ResumenConsumo> porTipoServicio,
                                   List<DetalleConsumo> detallePorRepuestoYServicio,
                                   int totalUnidades, BigDecimal valorComercialTotal) {
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        setPorRepuesto(porRepuesto);
        setPorTipoServicio(porTipoServicio);
        setDetallePorRepuestoYServicio(detallePorRepuestoYServicio);
        this.totalUnidades = totalUnidades;
        this.valorComercialTotal = valorComercialTotal != null ? valorComercialTotal : BigDecimal.ZERO;
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

    public List<ResumenConsumo> getPorRepuesto() {
        return porRepuesto;
    }

    public void setPorRepuesto(List<ResumenConsumo> porRepuesto) {
        this.porRepuesto = porRepuesto != null ? new ArrayList<>(porRepuesto) : new ArrayList<>();
    }

    public List<ResumenConsumo> getPorTipoServicio() {
        return porTipoServicio;
    }

    public void setPorTipoServicio(List<ResumenConsumo> porTipoServicio) {
        this.porTipoServicio = porTipoServicio != null ? new ArrayList<>(porTipoServicio) : new ArrayList<>();
    }

    public List<DetalleConsumo> getDetallePorRepuestoYServicio() {
        return detallePorRepuestoYServicio;
    }

    public void setDetallePorRepuestoYServicio(List<DetalleConsumo> detallePorRepuestoYServicio) {
        this.detallePorRepuestoYServicio = detallePorRepuestoYServicio != null
                ? new ArrayList<>(detallePorRepuestoYServicio)
                : new ArrayList<>();
    }

    public int getTotalUnidades() {
        return totalUnidades;
    }

    public void setTotalUnidades(int totalUnidades) {
        this.totalUnidades = totalUnidades;
    }

    public BigDecimal getValorComercialTotal() {
        return valorComercialTotal;
    }

    public void setValorComercialTotal(BigDecimal valorComercialTotal) {
        this.valorComercialTotal = valorComercialTotal != null ? valorComercialTotal : BigDecimal.ZERO;
    }

    public boolean estaVacio() {
        return porRepuesto.isEmpty();
    }

    /**
     * Resumen agregado de consumo por repuesto o por tipo de servicio.
     */
    public static class ResumenConsumo {
        private final int id;
        private final String nombre;
        private final int unidades;
        private final BigDecimal valorComercial;

        public ResumenConsumo(int id, String nombre, int unidades, BigDecimal valorComercial) {
            this.id = id;
            this.nombre = nombre;
            this.unidades = unidades;
            this.valorComercial = valorComercial;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public int getUnidades() {
            return unidades;
        }

        public BigDecimal getValorComercial() {
            return valorComercial;
        }

        @Override
        public String toString() {
            return "ResumenConsumo{" +
                    "id=" + id +
                    ", nombre='" + nombre + '\'' +
                    ", unidades=" + unidades +
                    ", valorComercial=" + valorComercial +
                    '}';
        }
    }

    /**
     * Desglose de consumo combinado por par (repuesto, tipo de servicio).
     */
    public static class DetalleConsumo {
        private final int repuestoId;
        private final String nombreRepuesto;
        private final int servicioTipoId;
        private final String nombreServicio;
        private final int unidades;
        private final BigDecimal valorComercial;

        public DetalleConsumo(int repuestoId, String nombreRepuesto,
                              int servicioTipoId, String nombreServicio,
                              int unidades, BigDecimal valorComercial) {
            this.repuestoId = repuestoId;
            this.nombreRepuesto = nombreRepuesto;
            this.servicioTipoId = servicioTipoId;
            this.nombreServicio = nombreServicio;
            this.unidades = unidades;
            this.valorComercial = valorComercial;
        }

        public int getRepuestoId() {
            return repuestoId;
        }

        public String getNombreRepuesto() {
            return nombreRepuesto;
        }

        public int getServicioTipoId() {
            return servicioTipoId;
        }

        public String getNombreServicio() {
            return nombreServicio;
        }

        public int getUnidades() {
            return unidades;
        }

        public BigDecimal getValorComercial() {
            return valorComercial;
        }

        @Override
        public String toString() {
            return "DetalleConsumo{" +
                    "repuestoId=" + repuestoId +
                    ", nombreRepuesto='" + nombreRepuesto + '\'' +
                    ", servicioTipoId=" + servicioTipoId +
                    ", nombreServicio='" + nombreServicio + '\'' +
                    ", unidades=" + unidades +
                    ", valorComercial=" + valorComercial +
                    '}';
        }
    }
}
