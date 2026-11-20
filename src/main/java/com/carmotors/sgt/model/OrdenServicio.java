package com.carmotors.sgt.model;

import java.time.LocalDateTime;

/**
 * Cabecera de una orden de servicio del taller (RF-08, RN-18).
 * Mapea la tabla {@code ordenes_servicio}.
 */
public class OrdenServicio {

    private int id;
    private int vehiculoId;
    private int estadoId;
    private LocalDateTime fechaRecepcion;
    private LocalDateTime fechaCompletado;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaCancelacion;
    private String descripcionProblema;

    public OrdenServicio() {
    }

    public OrdenServicio(int id, int vehiculoId, int estadoId, LocalDateTime fechaRecepcion,
                         String descripcionProblema) {
        this.id = id;
        this.vehiculoId = vehiculoId;
        this.estadoId = estadoId;
        this.fechaRecepcion = fechaRecepcion;
        this.descripcionProblema = descripcionProblema;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(int vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public int getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(int estadoId) {
        this.estadoId = estadoId;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public LocalDateTime getFechaCompletado() {
        return fechaCompletado;
    }

    public void setFechaCompletado(LocalDateTime fechaCompletado) {
        this.fechaCompletado = fechaCompletado;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getDescripcionProblema() {
        return descripcionProblema;
    }

    public void setDescripcionProblema(String descripcionProblema) {
        this.descripcionProblema = descripcionProblema;
    }

    @Override
    public String toString() {
        return "ID: " + id
                + " | Vehículo ID: " + vehiculoId
                + " | Estado ID: " + estadoId
                + " | Recepción: " + fechaRecepcion
                + " | Problema: " + descripcionProblema;
    }
}
