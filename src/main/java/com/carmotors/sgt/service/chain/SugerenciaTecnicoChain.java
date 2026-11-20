package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.service.TecnicoService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Orquestador de la cadena de responsabilidad para sugerir el técnico óptimo (RF-09, RN-20, RN-21).
 * Evalúa estrictamente en orden:
 * 1) Especialista libre (carga 0),
 * 2) Especialista con menor carga,
 * 3) Técnico general (otra especialidad) con menor carga.
 */
public class SugerenciaTecnicoChain {

    private final ManejadorSugerencia primerEslabon;

    public SugerenciaTecnicoChain() {
        ManejadorSugerencia especialistaLibre = new ManejadorEspecialistaLibre();
        ManejadorSugerencia especialistaMenorCarga = new ManejadorEspecialistaMenorCarga();
        ManejadorSugerencia generalMenorCarga = new ManejadorGeneralMenorCarga();

        especialistaLibre
                .establecerSiguiente(especialistaMenorCarga)
                .establecerSiguiente(generalMenorCarga);

        this.primerEslabon = especialistaLibre;
    }

    /**
     * Ejecuta la cadena de sugerencia sobre los técnicos recibidos y sus cargas actuales.
     *
     * @param especialidadRequeridaId ID de la especialidad requerida por el servicio (debe ser &gt; 0).
     * @param tecnicosActivos         lista de técnicos candidatos (los inactivos se excluyen).
     * @param cargasPorTecnico        mapa {@code tecnicoId -> cargaActual}; si es nulo o falta clave se asume 0.
     * @return la sugerencia encontrada o {@code null} si no hay técnicos activos disponibles.
     */
    public SugerenciaTecnico sugerir(
            int especialidadRequeridaId,
            List<Tecnico> tecnicosActivos,
            Map<Integer, Integer> cargasPorTecnico) {
        if (especialidadRequeridaId <= 0) {
            throw new ValidacionException("El ID de la especialidad requerida debe ser mayor que cero.");
        }

        List<Tecnico> listaSegura = tecnicosActivos == null
                ? Collections.emptyList()
                : tecnicosActivos;
        Map<Integer, Integer> cargasSeguras = cargasPorTecnico == null
                ? Collections.emptyMap()
                : cargasPorTecnico;

        return primerEslabon.sugerir(especialidadRequeridaId, listaSegura, cargasSeguras);
    }

    /**
     * Consulta los técnicos activos y sus cargas actuales mediante {@link TecnicoService}
     * y ejecuta la cadena de sugerencia (RF-09, RN-20, RN-21).
     *
     * @param especialidadRequeridaId ID de la especialidad requerida por el servicio (debe ser &gt; 0).
     * @param tecnicoService          servicio de técnicos ya configurado.
     * @return la sugerencia encontrada o {@code null} si no hay técnicos activos disponibles.
     */
    public SugerenciaTecnico sugerir(int especialidadRequeridaId, TecnicoService tecnicoService) {
        if (tecnicoService == null) {
            throw new ValidacionException("El servicio de técnicos es obligatorio para sugerir un técnico.");
        }
        return sugerir(
                especialidadRequeridaId,
                tecnicoService.listarActivos(),
                tecnicoService.obtenerCargasActivasPorTecnico());
    }
}
