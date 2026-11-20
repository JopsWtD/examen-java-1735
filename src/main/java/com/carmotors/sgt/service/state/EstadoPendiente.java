package com.carmotors.sgt.service.state;

public class EstadoPendiente extends EstadoOrdenBase {

    @Override
    public String getNombre() {
        return "Pendiente";
    }

    @Override
    public int getEstadoId() {
        return 1;
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoEnProceso || destino instanceof EstadoCancelada;
    }

    @Override
    public EstadoOrden iniciarProceso() {
        return new EstadoEnProceso();
    }

    @Override
    public EstadoOrden completar() {
        lanzarTransicionInvalida(new EstadoCompletado());
        return this;
    }

    @Override
    public EstadoOrden facturar() {
        lanzarTransicionInvalida(new EstadoFacturado());
        return this;
    }

    @Override
    public EstadoOrden entregar() {
        lanzarTransicionInvalida(new EstadoEntregado());
        return this;
    }

    @Override
    public EstadoOrden cancelar() {
        return new EstadoCancelada();
    }

    @Override
    public boolean permiteEdicion() {
        return true;
    }
}
