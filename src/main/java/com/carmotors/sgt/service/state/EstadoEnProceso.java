package com.carmotors.sgt.service.state;

public class EstadoEnProceso extends EstadoOrdenBase {

    @Override
    public String getNombre() {
        return "En Proceso";
    }

    @Override
    public int getEstadoId() {
        return 2;
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoCompletado || destino instanceof EstadoCancelada;
    }

    @Override
    public EstadoOrden iniciarProceso() {
        lanzarTransicionInvalida(new EstadoEnProceso());
        return this;
    }

    @Override
    public EstadoOrden completar() {
        return new EstadoCompletado();
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
