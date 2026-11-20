package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.RepuestoTipo;

import java.util.List;

/**
 * Contrato de persistencia de solo lectura para los tipos de repuesto.
 */
public interface RepuestoTipoDAO {

    /**
     * Retorna todos los tipos de repuesto ordenados por ID ascendente.
     *
     * @return lista de tipos de repuesto
     */
    List<RepuestoTipo> listarTodos();

    /**
     * Busca un tipo de repuesto por su identificador único.
     *
     * @param id identificador del tipo de repuesto
     * @return la entidad encontrada o {@code null} si no existe
     */
    RepuestoTipo buscarPorId(int id);
}
