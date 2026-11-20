package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.OrdenServicio;

import java.time.LocalDateTime;

/**
 * Contrato de persistencia para la cabecera de las órdenes de servicio ({@code ordenes_servicio}).
 */
public interface OrdenServicioDAO {

    /**
     * Inserta la cabecera de una orden y retorna el ID autogenerado.
     *
     * @param orden entidad a persistir
     * @return ID asignado en base de datos
     */
    int guardar(OrdenServicio orden);

    /**
     * Busca una orden por su identificador.
     *
     * @param id identificador de la orden
     * @return la orden encontrada o {@code null} si no existe
     */
    OrdenServicio buscarPorId(int id);

    /**
     * Busca y bloquea una orden dentro de una transacción JDBC activa ({@code FOR UPDATE}).
     *
     * @param id identificador de la orden
     * @return la orden encontrada o {@code null} si no existe
     */
    OrdenServicio buscarPorIdParaActualizar(int id);

    /**
     * Actualiza el estado de una orden existente (RF-12, RN-06).
     *
     * @param ordenId       identificador de la orden
     * @param nuevoEstadoId identificador del nuevo estado en {@code servicio_estados}
     * @return {@code true} si se actualizó al menos una fila
     */
    boolean actualizarEstado(int ordenId, int nuevoEstadoId);

    /**
     * Cambia el estado de la orden a Completado y registra su fecha de finalización
     * en una sola sentencia (RF-13).
     *
     * @param ordenId          identificador de la orden
     * @param estadoId         ID del estado Completado
     * @param fechaCompletado  fecha y hora de finalización
     * @return {@code true} si se actualizó la orden
     */
    boolean marcarCompletada(int ordenId, int estadoId, LocalDateTime fechaCompletado);

    /**
     * Cambia el estado de la orden a Cancelada y registra su fecha de cancelación
     * en una sola sentencia (RF-14, RN-06).
     *
     * @param ordenId          identificador de la orden
     * @param estadoId         ID del estado Cancelada
     * @param fechaCancelacion fecha y hora de cancelación
     * @return {@code true} si se actualizó la orden
     */
    boolean marcarCancelada(int ordenId, int estadoId, LocalDateTime fechaCancelacion);

    /**
     * Cambia el estado de la orden a Entregado y registra su fecha de entrega
     * en una sola sentencia (RF-12, RN-10).
     *
     * @param ordenId      identificador de la orden
     * @param estadoId     ID del estado Entregado
     * @param fechaEntrega fecha y hora de la entrega
     * @return {@code true} si se actualizó la orden
     */
    boolean marcarEntregada(int ordenId, int estadoId, LocalDateTime fechaEntrega);
}
