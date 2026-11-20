package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.Factura;

import java.util.List;

/**
 * Contrato de persistencia para la cabecera de las facturas ({@code facturas}).
 */
public interface FacturaDAO {

    /**
     * Inserta la cabecera de una factura y retorna el ID autogenerado.
     *
     * @param factura cabecera a persistir
     * @return ID asignado en base de datos
     */
    int guardar(Factura factura);

    /**
     * Busca una factura por su identificador.
     *
     * @return la factura encontrada o {@code null} si no existe
     */
    Factura buscarPorId(int id);

    /**
     * Busca la factura asociada a una orden (relación 1:1).
     *
     * @return la factura de la orden o {@code null} si la orden no ha sido facturada
     */
    Factura buscarPorOrden(int ordenId);

    /**
     * Busca una factura por su número único (ej. {@code F-2026-0001}).
     *
     * @param numeroFactura número de factura a buscar
     * @return la factura encontrada o {@code null} si no existe
     */
    Factura buscarPorNumero(String numeroFactura);

    /**
     * Lista todas las facturas registradas ordenadas de la más reciente a la más antigua.
     *
     * @return lista de cabeceras de facturas (vacía si no existen)
     */
    List<Factura> listarTodas();

    /** Marca una factura existente como generada, incluso si ya estaba marcada. */
    void marcarArchivoGenerado(int facturaId);

    /**
     * Obtiene el número de factura más alto emitido en un año (formato {@code F-<año>-<consecutivo>}).
     *
     * @param anio año de emisión
     * @return el último número del año o {@code null} si no hay facturas en ese año
     */
    String obtenerUltimoNumeroDelAnio(int anio);
}
