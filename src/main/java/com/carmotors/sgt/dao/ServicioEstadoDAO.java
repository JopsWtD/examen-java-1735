package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.ServicioEstado;

import java.util.List;

/**
 * Contrato de persistencia de solo lectura para los estados de servicio/orden.
 */
public interface ServicioEstadoDAO {

    /**
     * Retorna todos los estados de servicio ordenados por ID ascendente.
     *
     * @return lista de estados de servicio
     */
    List<ServicioEstado> listarTodos();

    /**
     * Busca un estado de servicio por su identificador único.
     *
     * @param id identificador del estado
     * @return la entidad encontrada o {@code null} si no existe
     */
    ServicioEstado buscarPorId(int id);
}
