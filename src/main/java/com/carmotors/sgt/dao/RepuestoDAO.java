package com.carmotors.sgt.dao;

import java.util.List;

import com.carmotors.sgt.model.Repuesto;

public interface RepuestoDAO {

    List<Repuesto> listarTodos();

    List<Repuesto> listarActivos();

    Repuesto buscarPorId(int id);

    /**
     * Busca y bloquea un repuesto dentro de una transacción JDBC activa ({@code FOR UPDATE}).
     *
     * @param id identificador del repuesto
     * @return el repuesto encontrado o {@code null} si no existe
     */
    default Repuesto buscarPorIdParaActualizar(int id) {
        return buscarPorId(id);
    }

    Repuesto buscarPorNombre(String nombre);

    int guardar(Repuesto repuesto);

    boolean actualizar(Repuesto repuesto);

    boolean desactivar(int id);

    boolean reactivar(int id);

    /**
     * Descuenta unidades reservadas de un repuesto dentro de una transacción activa.
     *
     * @param repuestoId identificador del repuesto
     * @param cantidad   unidades a liberar
     * @return {@code true} si existía reserva suficiente y se actualizó el registro
     */
    boolean descontarReserva(int repuestoId, int cantidad);

    /**
     * Suma unidades a {@code cantidad_reservada} del repuesto (RN-02). El cálculo lo hace MySQL;
     * no modifica la tabla {@code lotes}.
     *
     * @return {@code true} si se actualizó el repuesto
     */
    boolean aumentarReservado(int repuestoId, int cantidad);
}
