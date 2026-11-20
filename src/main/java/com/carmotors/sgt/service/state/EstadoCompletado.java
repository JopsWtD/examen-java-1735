package com.carmotors.sgt.service.state;

public class EstadoCompletado extends EstadoOrdenBase {

    @Override
    public String getNombre() {
        return "Completado";
    }

    @Override
    public int getEstadoId() {
        return 3;
    }

    @Override
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino instanceof EstadoFacturado;
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
        return new EstadoFacturado();
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
