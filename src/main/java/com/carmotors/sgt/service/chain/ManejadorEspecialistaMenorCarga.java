package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.model.Tecnico;

import java.util.Map;

/**
 * Eslabón 2 de la cadena de sugerencia (RN-20): busca un técnico activo de la especialidad
 * requerida con la menor carga laboral actual, desempatando por menor ID (RN-21).
 */
public class ManejadorEspecialistaMenorCarga extends ManejadorSugerencia {

    @Override
    protected boolean cumpleCriterio(
            int especialidadRequeridaId,
            Tecnico tecnico,
            Map<Integer, Integer> cargasPorTecnico) {
        return tecnico.getEspecialidadId() == especialidadRequeridaId;
    }

    @Override
    protected String obtenerCriterio() {
        return "Especialista con menor carga";
    }
}
