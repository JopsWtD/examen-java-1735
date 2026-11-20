package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.ServicioTipo;

import java.util.List;

/**
 * Contrato de persistencia para el catálogo de tipos de servicio ({@code servicio_tipos}).
 */
public interface ServicioTipoDAO {

    /**
     * Retorna todos los tipos de servicio ordenados por ID ascendente.
     *
     * @return lista de tipos de servicio
     */
    List<ServicioTipo> listarTodos();

    /**
     * Busca un tipo de servicio por su identificador único.
     *
     * @param id identificador del tipo de servicio
     * @return la entidad encontrada o {@code null} si no existe
     */
    ServicioTipo buscarPorId(int id);

    /**
     * Busca un tipo de servicio por nombre (sin distinguir mayúsculas/minúsculas).
     *
     * @param nombre nombre del servicio
     * @return la entidad encontrada o {@code null} si no existe
     */
    ServicioTipo buscarPorNombre(String nombre);

    /**
     * Registra un nuevo tipo de servicio y retorna el ID autogenerado.
     *
     * @param servicioTipo entidad a persistir
     * @return ID generado en base de datos
     */
    int guardar(ServicioTipo servicioTipo);

    /**
     * Actualiza un tipo de servicio existente.
     *
     * @param servicioTipo entidad con los valores modificados
     * @return {@code true} si se actualizó al menos un registro
     */
    boolean actualizar(ServicioTipo servicioTipo);

    /**
     * Elimina un tipo de servicio por ID siempre que no tenga referencias en órdenes.
     *
     * @param id identificador del tipo de servicio
     * @return {@code true} si se eliminó el registro
     */
    boolean eliminar(int id);
}
