package com.carmotors.sgt.service.state;

public class EstadoCancelada extends EstadoOrdenBase {

    @Override
    public String getNombre() {
        return "Cancelada";
    }

    @Override
    public int getEstadoId() {
        return 6;
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return false;
    }

    @Override
    public EstadoOrden iniciarProceso() {
        lanzarTransicionInvalida(new EstadoEnProceso());
        return this;
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
        lanzarTransicionInvalida(new EstadoCancelada());
        return this;
    }

    @Override
    public boolean permiteEdicion() {
        return false;
    }
}
