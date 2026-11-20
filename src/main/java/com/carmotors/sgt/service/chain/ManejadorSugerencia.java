package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Tecnico;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador base del patrón Chain of Responsibility para sugerir técnicos (RF-09, RN-20, RN-21).
 * Cada eslabón evalúa su criterio de selección y, si no encuentra candidato, delega al siguiente.
 */
public abstract class ManejadorSugerencia {

    protected ManejadorSugerencia siguiente;

    /**
     * Enlaza el siguiente eslabón de la cadena y lo retorna para permitir encadenamiento fluido.
     */
    public ManejadorSugerencia establecerSiguiente(ManejadorSugerencia siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    /**
     * Valida las entradas, ejecuta el criterio del eslabón actual y delega al siguiente
     * únicamente si no se encontró ningún candidato.
     */
    public SugerenciaTecnico sugerir(
            int especialidadRequeridaId,
            List<Tecnico> tecnicosActivos,
            Map<Integer, Integer> cargasPorTecnico) {
        if (especialidadRequeridaId <= 0) {
            throw new ValidacionException("El ID de la especialidad requerida debe ser mayor que cero.");
        }

        List<Tecnico> listaNormalizada = tecnicosActivos == null
                ? Collections.emptyList()
                : tecnicosActivos;
        Map<Integer, Integer> cargasNormalizadas = cargasPorTecnico == null
                ? Collections.emptyMap()
                : cargasPorTecnico;

        SugerenciaTecnico sugerencia = buscarCandidato(
                especialidadRequeridaId,
                listaNormalizada,
                cargasNormalizadas);
        if (sugerencia != null) {
            return sugerencia;
        }
        if (siguiente != null) {
            return siguiente.sugerir(especialidadRequeridaId, listaNormalizada, cargasNormalizadas);
        }
        return null;
    }

    /**
     * Evalúa los técnicos activos con el criterio del eslabón actual y construye la sugerencia ganadora.
     */
    protected SugerenciaTecnico buscarCandidato(
            int especialidadRequeridaId,
            List<Tecnico> tecnicosActivos,
            Map<Integer, Integer> cargasPorTecnico) {
        Tecnico elegido = seleccionarMenorCargaYMenorId(
                especialidadRequeridaId,
                tecnicosActivos,
                cargasPorTecnico);
        if (elegido == null) {
            return null;
        }
        int carga = obtenerCarga(elegido, cargasPorTecnico);
        boolean especialidadCoincide = elegido.getEspecialidadId() == especialidadRequeridaId;
        return new SugerenciaTecnico(elegido, carga, especialidadCoincide, obtenerCriterio());
    }

    /**
     * Define si un técnico activo cumple la condición particular del eslabón.
     */
    protected abstract boolean cumpleCriterio(
            int especialidadRequeridaId,
            Tecnico tecnico,
            Map<Integer, Integer> cargasPorTecnico);

    /**
     * Descripción legible del criterio aplicado por el eslabón ganador.
     */
    protected abstract String obtenerCriterio();

    /**
     * Obtiene la carga laboral actual de un técnico (0 si no tiene asignaciones registradas).
     */
    protected int obtenerCarga(Tecnico tecnico, Map<Integer, Integer> cargasPorTecnico) {
        if (tecnico == null || cargasPorTecnico == null) {
            return 0;
        }
        Integer carga = cargasPorTecnico.get(tecnico.getId());
        if (carga == null || carga < 0) {
            return 0;
        }
        return carga;
    }

    /**
     * Filtra técnicos activos según el criterio del eslabón y selecciona el de menor carga actual,
     * desempatando de forma determinista por el menor {@code tecnico.getId()} (RN-21).
     */
    protected Tecnico seleccionarMenorCargaYMenorId(
            int especialidadRequeridaId,
            List<Tecnico> tecnicosActivos,
            Map<Integer, Integer> cargasPorTecnico) {
        if (tecnicosActivos == null || tecnicosActivos.isEmpty()) {
            return null;
        }

        Comparator<Tecnico> comparador = Comparator
                .comparingInt((Tecnico t) -> obtenerCarga(t, cargasPorTecnico))
                .thenComparingInt(Tecnico::getId);

        List<Tecnico> candidatos = tecnicosActivos.stream()
                .filter(t -> t != null && t.isActivo()
                        && cumpleCriterio(especialidadRequeridaId, t, cargasPorTecnico))
                .sorted(comparador)
                .collect(Collectors.toList());
        return candidatos.isEmpty() ? null : candidatos.get(0);
    }
}
