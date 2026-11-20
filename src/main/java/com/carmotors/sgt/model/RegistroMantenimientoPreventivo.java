package com.carmotors.sgt.model;

import java.time.LocalDateTime;

/**
 * Registro plano para los recordatorios de mantenimiento (RF-21): una fila por cada línea de un
 * servicio preventivo con intervalo definido que pertenece a una orden Entregada.
 */
public class RegistroMantenimientoPreventivo {
    private final int vehiculoId;
    private final String placa;
    private final String marca;
    private final String modelo;
    private final int clienteId;
    private final String clienteNombre;
    private final String clienteTelefono;
    private final String clienteEmail;
    private final boolean clienteActivo;
    private final int servicioTipoId;
    private final String nombreServicio;
    private final Integer intervaloMeses;
    private final int ordenId;
    private final LocalDateTime fechaEntrega;

    public RegistroMantenimientoPreventivo(int vehiculoId, String placa, String marca, String modelo, int clienteId,
                                           String clienteNombre, String clienteTelefono, String clienteEmail,
                                           boolean clienteActivo, int servicioTipoId, String nombreServicio,
                                           Integer intervaloMeses, int ordenId, LocalDateTime fechaEntrega) {
        this.vehiculoId = vehiculoId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.clienteActivo = clienteActivo;
        this.servicioTipoId = servicioTipoId;
        this.nombreServicio = nombreServicio;
        this.intervaloMeses = intervaloMeses;
        this.ordenId = ordenId;
        this.fechaEntrega = fechaEntrega;
    }

    public int getVehiculoId() {
        return vehiculoId;
    }

    public String getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public int getClienteId() {
        return clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public String getClienteTelefono() {
        return clienteTelefono;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public boolean isClienteActivo() {
        return clienteActivo;
    }

    public int getServicioTipoId() {
        return servicioTipoId;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public Integer getIntervaloMeses() {
        return intervaloMeses;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    @Override
    public String toString() {
        return "RegistroMantenimientoPreventivo{" +
                "vehiculoId=" + vehiculoId +
                ", placa='" + placa + '\'' +
                ", clienteId=" + clienteId +
                ", clienteActivo=" + clienteActivo +
                ", servicioTipoId=" + servicioTipoId +
                ", nombreServicio='" + nombreServicio + '\'' +
                ", intervaloMeses=" + intervaloMeses +
                ", ordenId=" + ordenId +
                ", fechaEntrega=" + fechaEntrega +
                '}';
    }
}
