package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.Cliente;

import java.util.List;

/**
 * Contrato de persistencia para la gestión de clientes del taller (RF-17, RN-13, RN-17).
 */
public interface ClienteDAO {

    /**
     * Retorna todos los clientes registrados ordenados por ID ascendente.
     *
     * @return lista completa de clientes (activos e inactivos)
     */
    List<Cliente> listarTodos();

    /**
     * Retorna únicamente los clientes activos ordenados por ID ascendente.
     *
     * @return lista de clientes activos
     */
    List<Cliente> listarActivos();

    /**
     * Busca un cliente por su identificador único.
     *
     * @param id identificador del cliente
     * @return el cliente encontrado o {@code null} si no existe
     */
    Cliente buscarPorId(int id);

    /**
     * Busca un cliente por su documento de identidad exacto.
     *
     * @param documento documento de identidad
     * @return el cliente encontrado o {@code null} si no existe
     */
    Cliente buscarPorDocumento(String documento);

    /**
     * Busca un cliente por su email exacto (RN-13).
     *
     * @param email correo electrónico normalizado
     * @return el cliente encontrado o {@code null} si no existe
     */
    Cliente buscarPorEmail(String email);

    /**
     * Inserta un nuevo cliente y retorna el ID autogenerado.
     *
     * @param cliente entidad a registrar
     * @return ID generado en base de datos
     */
    int guardar(Cliente cliente);

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param cliente entidad con los datos actualizados
     * @return {@code true} si se actualizó al menos una fila
     */
    boolean actualizar(Cliente cliente);

    /**
     * Cambia el estado de activación lógica de un cliente (RN-17).
     *
     * @param id identificador del cliente
     * @param activo {@code true} para reactivar, {@code false} para desactivar
     * @return {@code true} si el registro fue actualizado
     */
    boolean actualizarEstadoActivo(int id, boolean activo);
}
