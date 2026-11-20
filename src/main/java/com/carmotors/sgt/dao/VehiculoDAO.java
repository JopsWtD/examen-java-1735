package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.Vehiculo;

import java.util.List;

/**
 * Contrato de persistencia para la gestión de vehículos (RF-18, RN-13, RN-17).
 */
public interface VehiculoDAO {

    /**
     * Inserta un nuevo vehículo y retorna el ID autogenerado.
     *
     * @param vehiculo entidad a persistir
     * @return ID asignado en base de datos
     */
    int guardar(Vehiculo vehiculo);

    /**
     * Busca un vehículo por su identificador primario.
     *
     * @param id identificador único del vehículo
     * @return la entidad encontrada o {@code null} si no existe
     */
    Vehiculo buscarPorId(int id);

    /**
     * Busca un vehículo por su placa normalizada (RN-13).
     *
     * @param placa placa normalizada (mayúsculas, sin espacios ni guiones)
     * @return la entidad encontrada o {@code null} si no existe
     */
    Vehiculo buscarPorPlaca(String placa);

    /**
     * Retorna todos los vehículos asociados a un cliente específico.
     *
     * @param clienteId identificador del cliente propietario
     * @return lista de vehículos del cliente ordenados por ID ascendente
     */
    List<Vehiculo> listarPorCliente(int clienteId);

    /**
     * Retorna todos los vehículos registrados en el taller.
     *
     * @return lista de todos los vehículos
     */
    List<Vehiculo> listarTodos();
}
