package com.carmotors.sgt.model;

import java.math.BigDecimal;

/**
 * DTO que representa una línea de servicio activa asignada a un técnico
 * en órdenes con estado Pendiente o En Proceso (RF-34, RN-21, HU-16).
 */
public class CargaLineaTecnico {

    private int ordenId;
    private int detalleId;
    private String placa;
    private String tipoServicio;
    private int estadoId;
    private String estadoNombre;
    private BigDecimal horasTrabajadas;

    public CargaLineaTecnico() {
    }

    public CargaLineaTecnico(int ordenId, int detalleId, String placa, String tipoServicio,
                             int estadoId, String estadoNombre, BigDecimal horasTrabajadas) {
        this.ordenId = ordenId;
        this.detalleId = detalleId;
        this.placa = placa;
        this.tipoServicio = tipoServicio;
        this.estadoId = estadoId;
        this.estadoNombre = estadoNombre;
        this.horasTrabajadas = horasTrabajadas;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(int ordenId) {
        this.ordenId = ordenId;
    }

    public int getDetalleId() {
        return detalleId;
    }

    public void setDetalleId(int detalleId) {
        this.detalleId = detalleId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public int getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(int estadoId) {
        this.estadoId = estadoId;
    }

    public String getEstadoNombre() {
        return estadoNombre;
    }

    public void setEstadoNombre(String estadoNombre) {
        this.estadoNombre = estadoNombre;
    }

    public BigDecimal getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(BigDecimal horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    @Override
    public String toString() {
        return "Orden: " + ordenId
                + " | Detalle: " + detalleId
                + " | Placa: " + placa
                + " | Servicio: " + tipoServicio
                + " | Estado: " + estadoNombre + " (" + estadoId + ")"
                + " | Horas: " + horasTrabajadas;
    }
}
