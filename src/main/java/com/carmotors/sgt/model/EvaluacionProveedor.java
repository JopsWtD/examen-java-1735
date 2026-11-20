package com.carmotors.sgt.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class EvaluacionProveedor {

    private int id;
    private int proveedorId;
    private LocalDate fecha = LocalDate.now();
    private int calidad;
    private int puntualidad;
    private String comentario;

    public EvaluacionProveedor() {
    }

    public EvaluacionProveedor(int id, int proveedorId, LocalDate fecha, int calidad, int puntualidad, String comentario) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.fecha = fecha != null ? fecha : LocalDate.now();
        this.calidad = calidad;
        this.puntualidad = puntualidad;
        this.comentario = comentario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha != null ? fecha : LocalDate.now();
    }

    public int getCalidad() {
        return calidad;
    }

    public void setCalidad(int calidad) {
        this.calidad = calidad;
    }

    public int getPuntualidad() {
        return puntualidad;
    }

    public void setPuntualidad(int puntualidad) {
        this.puntualidad = puntualidad;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public BigDecimal getPuntaje() {
        if (calidad < 0 || puntualidad < 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal promedio = BigDecimal.valueOf((calidad + puntualidad) / 2.0)
                .setScale(1, RoundingMode.HALF_UP);
        return promedio;
    }

    @Override
    public String toString() {
        return "EvaluacionProveedor{" +
                "id=" + id +
                ", proveedorId=" + proveedorId +
                ", fecha=" + fecha +
                ", calidad=" + calidad +
                ", puntualidad=" + puntualidad +
                ", comentario='" + comentario + '\'' +
                '}';
    }
}
