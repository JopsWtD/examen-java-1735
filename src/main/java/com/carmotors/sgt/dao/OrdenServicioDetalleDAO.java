package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicioDetalle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Contrato de persistencia para las líneas de servicio de una orden ({@code orden_servicio_detalle}).
 */
public interface OrdenServicioDetalleDAO {

    /**
     * Inserta una línea de servicio y retorna el ID autogenerado.
     *
     * @param detalle línea a persistir (con {@code ordenId} ya asignado)
     * @return ID asignado en base de datos
     */
    int guardar(OrdenServicioDetalle detalle);

    /**
     * Retorna las líneas de una orden ordenadas por ID ascendente.
     *
     * @param ordenId identificador de la orden
     * @return lista de líneas (vacía si la orden no tiene líneas)
     */
    List<OrdenServicioDetalle> listarPorOrden(int ordenId);

    /**
     * Lista y bloquea las líneas de una orden dentro de una transacción ({@code FOR UPDATE}).
     *
     * @param ordenId identificador de la orden
     * @return lista de líneas de la orden
     */
    List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId);

    /**
     * Asocia un técnico a una línea de servicio recién creada con horas iniciales en {@code 0.00}.
     *
     * @param detalleId identificador de la línea
     * @param tecnicoId identificador del técnico activo
     */
    void asignarTecnico(int detalleId, int tecnicoId);

    /**
     * Asocia un técnico a una línea de servicio con el registro inicial de horas trabajadas (RF-09).
     *
     * @param detalleId       identificador de la línea
     * @param tecnicoId       identificador del técnico activo
     * @param horasTrabajadas horas trabajadas iniciales (no negativas)
     */
    void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas);

    /**
     * Verifica si un técnico ya se encuentra asignado a la línea indicada (RF-09).
     *
     * @param detalleId identificador de la línea
     * @param tecnicoId identificador del técnico
     * @return {@code true} si ya existe la asignación en {@code detalle_tecnicos}
     */
    boolean existeTecnicoAsignado(int detalleId, int tecnicoId);

    /**
     * Retorna las asignaciones de técnicos de una línea ordenadas por {@code tecnico_id} ascendente.
     *
     * @param detalleId identificador de la línea
     * @return lista de asignaciones con sus horas trabajadas
     */
    List<DetalleTecnico> listarTecnicosAsignados(int detalleId);

    /**
     * Actualiza las horas trabajadas registradas para un técnico en una línea (RF-09).
     *
     * @param detalleId       identificador de la línea
     * @param tecnicoId       identificador del técnico asignado
     * @param horasTrabajadas nuevo total de horas trabajadas (no negativo)
     * @return {@code true} si se actualizó la fila correspondiente
     */
    boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas);

    /**
     * Indica si una línea tiene horas trabajadas mayores a cero registradas.
     *
     * @param detalleId identificador de la línea
     * @return {@code true} si existe algún técnico con horas mayores a cero en la línea
     */
    boolean tieneHorasTrabajadas(int detalleId);

    /**
     * Obtiene y bloquea las reservas de repuestos asociadas a una línea.
     *
     * @param detalleId identificador de la línea
     * @return mapa {@code repuestoId -> cantidadAsignada}
     */
    Map<Integer, Integer> listarReservasParaActualizar(int detalleId);

    /**
     * Actualiza el costo de mano de obra de una línea dentro de una orden editable (RN-08).
     *
     * @param ordenId       identificador de la orden
     * @param detalleId     identificador de la línea
     * @param costoManoObra nuevo costo de mano de obra (mayor o igual a cero)
     * @return {@code true} si se actualizó exactamente la fila indicada
     */
    default boolean actualizarCostoManoObra(int ordenId, int detalleId, BigDecimal costoManoObra) {
        throw new UnsupportedOperationException("actualizarCostoManoObra no implementado en este DAO.");
    }

    /**
     * Elimina una línea de servicio perteneciente a la orden indicada.
     *
     * @param ordenId   identificador de la orden
     * @param detalleId identificador de la línea
     */
    void eliminar(int ordenId, int detalleId);

    /**
     * Busca una línea de servicio por su identificador.
     *
     * @param id identificador de la línea
     * @return la línea encontrada o {@code null} si no existe
     */
    OrdenServicioDetalle buscarPorId(int id);
}
