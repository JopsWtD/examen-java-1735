package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.DetalleRepuesto;

import java.util.List;

/**
 * Contrato de persistencia para los repuestos reservados en las líneas de servicio
 * ({@code detalle_repuestos}, clave compuesta detalle_id + repuesto_id).
 */
public interface DetalleRepuestoDAO {

    /**
     * Inserta la reserva de un repuesto en una línea.
     *
     * @param detalleRepuesto reserva con el snapshot de nombre y precio ya asignado
     * @return {@code true} si se insertó la fila
     */
    boolean guardar(DetalleRepuesto detalleRepuesto);

    /**
     * Busca la reserva de un repuesto en una línea.
     *
     * @return la reserva encontrada o {@code null} si el repuesto no está asignado a esa línea
     */
    DetalleRepuesto buscar(int detalleId, int repuestoId);

    /**
     * Retorna los repuestos reservados en una línea ordenados por ID de repuesto.
     *
     * @param detalleId identificador de la línea de servicio
     * @return lista de reservas (vacía si la línea no tiene repuestos)
     */
    List<DetalleRepuesto> listarPorDetalle(int detalleId);

    /**
     * Elimina la reserva de un repuesto en una línea de servicio (RF-11, RN-05).
     *
     * @param detalleId  identificador de la línea de servicio
     * @param repuestoId identificador del repuesto reservado
     * @return {@code true} si se eliminó la fila de {@code detalle_repuestos}
     */
    boolean eliminar(int detalleId, int repuestoId);
}
