package com.carmotors.sgt.service.state;

public class EstadoFacturado extends EstadoOrdenBase {

    @Override
    public String getNombre() {
        return "Facturado";
    }

    @Override
    public int getEstadoId() {
        return 4;
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoEntregado;
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
        return new EstadoEntregado();
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
