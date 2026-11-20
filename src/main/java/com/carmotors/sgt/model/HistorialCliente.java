package com.carmotors.sgt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Historial de servicio de un cliente: sus datos, sus vehículos y sus órdenes de servicio
 * ordenadas de la más reciente a la más antigua (RF-20, RN-17, HU-10).
 */
public class HistorialCliente {

    private Cliente cliente;
    private List<Vehiculo> vehiculos = new ArrayList<>();
    private List<ResumenOrdenHistorial> ordenes = new ArrayList<>();

    public HistorialCliente() {
    }

    public HistorialCliente(Cliente cliente, List<Vehiculo> vehiculos, List<ResumenOrdenHistorial> ordenes) {
        this.cliente = cliente;
        this.vehiculos = vehiculos;
        this.ordenes = ordenes;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public void setVehiculos(List<Vehiculo> vehiculos) {
        this.vehiculos = vehiculos;
    }

    public List<ResumenOrdenHistorial> getOrdenes() {
        return ordenes;
    }

    public void setOrdenes(List<ResumenOrdenHistorial> ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public String toString() {
        return "HistorialCliente{" +
                "cliente=" + (cliente != null ? cliente.getNombre() : null) +
                ", vehiculos=" + vehiculos.size() +
                ", ordenes=" + ordenes.size() +
                '}';
    }
}
