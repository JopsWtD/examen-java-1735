package com.carmotors.sgt.model;

/**
 * Datos históricos del cliente y del vehículo al momento de facturar ({@code factura_snapshot}).
 * Relación 1:1 con la factura: {@code facturaId} es a la vez PK y FK (sección 2.7).
 */
public class FacturaSnapshot {
    private int facturaId;
    private String clienteNombre;
    private String clienteDocumento;
    private String clienteTelefono;
    private String clienteEmail;
    private String vehiculoPlaca;
    private String vehiculoMarca;
    private String vehiculoModelo;

    public FacturaSnapshot() {
    }

    public FacturaSnapshot(int facturaId, String clienteNombre, String clienteDocumento, String clienteTelefono,
                           String clienteEmail, String vehiculoPlaca, String vehiculoMarca,
                           String vehiculoModelo) {
        this.facturaId = facturaId;
        this.clienteNombre = clienteNombre;
        this.clienteDocumento = clienteDocumento;
        this.clienteTelefono = clienteTelefono;
        this.clienteEmail = clienteEmail;
        this.vehiculoPlaca = vehiculoPlaca;
        this.vehiculoMarca = vehiculoMarca;
        this.vehiculoModelo = vehiculoModelo;
    }

    public int getFacturaId() {
        return facturaId;
    }

    public void setFacturaId(int facturaId) {
        this.facturaId = facturaId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
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

    public String getVehiculoPlaca() {
        return vehiculoPlaca;
    }

    public void setVehiculoPlaca(String vehiculoPlaca) {
        this.vehiculoPlaca = vehiculoPlaca;
    }

    public String getVehiculoMarca() {
        return vehiculoMarca;
    }

    public void setVehiculoMarca(String vehiculoMarca) {
        this.vehiculoMarca = vehiculoMarca;
    }

    public String getVehiculoModelo() {
        return vehiculoModelo;
    }

    public void setVehiculoModelo(String vehiculoModelo) {
        this.vehiculoModelo = vehiculoModelo;
    }

    @Override
    public String toString() {
        return "FacturaSnapshot{" +
                "facturaId=" + facturaId +
                ", clienteNombre='" + clienteNombre + '\'' +
                ", clienteDocumento='" + clienteDocumento + '\'' +
                ", clienteTelefono='" + clienteTelefono + '\'' +
                ", clienteEmail='" + clienteEmail + '\'' +
                ", vehiculoPlaca='" + vehiculoPlaca + '\'' +
                ", vehiculoMarca='" + vehiculoMarca + '\'' +
                ", vehiculoModelo='" + vehiculoModelo + '\'' +
                '}';
    }
}
