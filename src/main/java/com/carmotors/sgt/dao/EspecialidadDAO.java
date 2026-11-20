package com.carmotors.sgt.dao;

import com.carmotors.sgt.model.Especialidad;

import java.util.List;

/**
 * Contrato de persistencia de solo lectura para las especialidades técnicas.
 */
public interface EspecialidadDAO {

    /**
     * Retorna todas las especialidades técnicas ordenadas por ID ascendente.
     *
     * @return lista de especialidades
     */
    List<Especialidad> listarTodos();

    /**
     * Busca una especialidad técnica por su identificador único.
     *
     * @param id identificador de la especialidad
     * @return la entidad encontrada o {@code null} si no existe
     */
    Especialidad buscarPorId(int id);
}
