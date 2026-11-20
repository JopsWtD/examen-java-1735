package com.carmotors.sgt.model;

import java.time.LocalDate;

/**
 * Recordatorio de mantenimiento preventivo para un vehículo y un tipo de servicio (RF-21, RN-15).
 *
 * <p>{@code diasRestantes} es la diferencia en días entre la fecha de referencia y la fecha sugerida:
 * positiva o cero si el mantenimiento está próximo y negativa si ya venció.</p>
 */
public class RecordatorioMantenimiento {
    private int clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private String clienteEmail;
    private int vehiculoId;
    private String placa;
    private String marca;
    private String modelo;
    private int servicioTipoId;
    private String nombreServicio;
    private int intervaloMeses;
    private int ordenId;
    private LocalDate fechaUltimaEntrega;
    private LocalDate fechaSugerida;
    private int diasRestantes;

    public RecordatorioMantenimiento() {
    }

    public RecordatorioMantenimiento(int clienteId, String clienteNombre, String clienteTelefono, String clienteEmail,
                                     int vehiculoId, String placa, String marca, String modelo, int servicioTipoId,
                                     String nombreServicio, int intervaloMeses, int ordenId,
                                     LocalDate fechaUltimaEntrega, LocalDate fechaSugerida, int diasRestantes) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.vehiculoId = vehiculoId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.servicioTipoId = servicioTipoId;
        this.nombreServicio = nombreServicio;
        this.intervaloMeses = intervaloMeses;
        this.ordenId = ordenId;
        this.fechaUltimaEntrega = fechaUltimaEntrega;
        this.fechaSugerida = fechaSugerida;
        this.diasRestantes = diasRestantes;
    }

    /** Indica si la fecha sugerida ya pasó respecto a la fecha de referencia. */
    public boolean isVencido() {
        return diasRestantes < 0;
    }

    /** Días de atraso si está vencido; cero en caso contrario. */
    public int getDiasAtraso() {
        return isVencido() ? -diasRestantes : 0;
    }

    public int getClienteId() {
        return clienteId;
    }

    public void setClienteId(int clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteTelefono() {
        return clienteTelefono;
    }

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public int getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(int vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getServicioTipoId() {
        return servicioTipoId;
    }

    public void setServicioTipoId(int servicioTipoId) {
        this.servicioTipoId = servicioTipoId;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public int getIntervaloMeses() {
        return intervaloMeses;
    }

    public void setIntervaloMeses(int intervaloMeses) {
        this.intervaloMeses = intervaloMeses;
    }

    public int getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(int ordenId) {
        this.ordenId = ordenId;
    }

    public LocalDate getFechaUltimaEntrega() {
        return fechaUltimaEntrega;
    }

    public void setFechaUltimaEntrega(LocalDate fechaUltimaEntrega) {
        this.fechaUltimaEntrega = fechaUltimaEntrega;
    }

    public LocalDate getFechaSugerida() {
        return fechaSugerida;
    }

    public void setFechaSugerida(LocalDate fechaSugerida) {
        this.fechaSugerida = fechaSugerida;
    }

    public int getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(int diasRestantes) {
        this.diasRestantes = diasRestantes;
    }

    @Override
    public String toString() {
        return "RecordatorioMantenimiento{" +
                "clienteNombre='" + clienteNombre + '\'' +
                ", placa='" + placa + '\'' +
                ", nombreServicio='" + nombreServicio + '\'' +
                ", fechaUltimaEntrega=" + fechaUltimaEntrega +
                ", fechaSugerida=" + fechaSugerida +
                ", diasRestantes=" + diasRestantes +
                '}';
    }
}
