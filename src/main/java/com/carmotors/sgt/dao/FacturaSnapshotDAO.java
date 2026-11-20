package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.FacturaSnapshot;

/**
 * Contrato de persistencia para los datos históricos de cliente y vehículo de una factura
 * ({@code factura_snapshot}).
 */
public interface FacturaSnapshotDAO {

    /**
     * Inserta el snapshot de una factura.
     *
     * @return {@code true} si se insertó la fila
     */
    boolean guardar(FacturaSnapshot snapshot);

    /**
     * Busca el snapshot de una factura.
     *
     * @return el snapshot encontrado o {@code null} si no existe
     */
    FacturaSnapshot buscarPorFactura(int facturaId);
}
