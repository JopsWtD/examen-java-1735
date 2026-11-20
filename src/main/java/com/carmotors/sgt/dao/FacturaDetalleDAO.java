package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.FacturaDetalle;

import java.util.List;

/**
 * Contrato de persistencia para los conceptos cobrados en una factura ({@code factura_detalles}).
 */
public interface FacturaDetalleDAO {

    /**
     * Inserta un concepto y retorna el ID autogenerado.
     *
     * @param detalle concepto a persistir (con {@code facturaId} ya asignado)
     * @return ID asignado en base de datos
     */
    int guardar(FacturaDetalle detalle);

    /**
     * Retorna los conceptos de una factura ordenados por ID ascendente.
     *
     * @param facturaId identificador de la factura
     * @return lista de conceptos (vacía si la factura no tiene conceptos)
     */
    List<FacturaDetalle> listarPorFactura(int facturaId);
}
