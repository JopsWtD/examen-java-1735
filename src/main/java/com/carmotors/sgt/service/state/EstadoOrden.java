package com.carmotors.sgt.service.state;

/**
 * Contrato del patrón State para el ciclo de vida de una orden de servicio.
 */
public interface EstadoOrden {

    String getNombre();

    int getEstadoId();

    boolean puedeTransicionarA(EstadoOrden destino);

    EstadoOrden transicionarA(EstadoOrden destino);

    EstadoOrden iniciarProceso();

    EstadoOrden completar();

    EstadoOrden facturar();

    EstadoOrden entregar();

    EstadoOrden cancelar();

    boolean permiteEdicion();
}
