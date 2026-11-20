package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.model.Tecnico;

import java.util.Map;

/**
 * Eslabón 3 de la cadena de sugerencia (RN-20): cuando no hay especialistas activos disponibles,
 * busca cualquier técnico activo con la menor carga laboral actual, desempatando por menor ID (RN-21).
 */
public class ManejadorGeneralMenorCarga extends ManejadorSugerencia {

    @Override
    protected boolean cumpleCriterio(
            int especialidadRequeridaId,
            Tecnico tecnico,
            Map<Integer, Integer> cargasPorTecnico) {
        return true;
    }

    @Override
    protected String obtenerCriterio() {
        return "Técnico de otra especialidad con menor carga";
    }
}
