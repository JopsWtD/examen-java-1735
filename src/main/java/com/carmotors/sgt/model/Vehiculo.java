package com.carmotors.sgt.model;

/**
 * Entidad que representa a un vehículo registrado en el taller (RF-18, RN-13, RN-17).
 * Mapea la tabla {@code vehiculos}.
 */
public class Vehiculo {

    private int id;
    private int clienteId;
    private String placa;
    private String marca;
    private String modelo;

    public Vehiculo() {
    }

    public Vehiculo(int clienteId, String placa, String marca, String modelo) {
        this.clienteId = clienteId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
    }

    public Vehiculo(int id, int clienteId, String placa, String marca, String modelo) {
        this.id = id;
        this.clienteId = clienteId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public void setClienteId(int clienteId) {
        this.clienteId = clienteId;
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

    @Override
    public String toString() {
        return "ID: " + id
                + " | Placa: " + placa
                + " | Marca: " + marca
                + " | Modelo: " + modelo
                + " | Cliente ID: " + clienteId;
    }
}
