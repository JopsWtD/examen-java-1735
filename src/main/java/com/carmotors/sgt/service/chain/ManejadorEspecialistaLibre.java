package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.model.Tecnico;

import java.util.Map;

/**
 * Eslabón 1 de la cadena de sugerencia (RN-20): busca un técnico activo de la especialidad
 * requerida cuya carga laboral actual sea exactamente 0, desempatando por menor ID (RN-21).
 */
public class ManejadorEspecialistaLibre extends ManejadorSugerencia {

    @Override
    protected boolean cumpleCriterio(
            int especialidadRequeridaId,
            Tecnico tecnico,
            Map<Integer, Integer> cargasPorTecnico) {
        return tecnico.getEspecialidadId() == especialidadRequeridaId
                && obtenerCarga(tecnico, cargasPorTecnico) == 0;
    }

    @Override
    protected String obtenerCriterio() {
        return "Especialista libre";
    }
}
