package com.carmotors.sgt.service.chain;

import com.carmotors.sgt.model.Tecnico;

/**
 * Resultado de la cadena de sugerencia de técnicos (RF-09, RN-20, RN-21).
 * Transporta el técnico recomendado junto con su carga actual, si coincide
 * con la especialidad requerida y el criterio aplicado por el eslabón ganador.
 */
public class SugerenciaTecnico {

    private Tecnico tecnico;
    private int cargaActual;
    private boolean especialidadCoincide;
    private String criterioAplicado;

    public SugerenciaTecnico() {
    }

    public SugerenciaTecnico(
            Tecnico tecnico,
            int cargaActual,
            boolean especialidadCoincide,
            String criterioAplicado) {
        this.tecnico = tecnico;
        this.cargaActual = cargaActual;
        this.especialidadCoincide = especialidadCoincide;
        this.criterioAplicado = criterioAplicado;
    }

    public Tecnico getTecnico() {
        return tecnico;
    }

    public void setTecnico(Tecnico tecnico) {
        this.tecnico = tecnico;
    }

    public int getCargaActual() {
        return cargaActual;
    }

    public void setCargaActual(int cargaActual) {
        this.cargaActual = cargaActual;
    }

    public boolean isEspecialidadCoincide() {
        return especialidadCoincide;
    }

    public void setEspecialidadCoincide(boolean especialidadCoincide) {
        this.especialidadCoincide = especialidadCoincide;
    }

    public String getCriterioAplicado() {
        return criterioAplicado;
    }

    public void setCriterioAplicado(String criterioAplicado) {
        this.criterioAplicado = criterioAplicado;
    }

    @Override
    public String toString() {
        String nombreTecnico = tecnico == null ? "N/A" : tecnico.getNombre();
        int idTecnico = tecnico == null ? 0 : tecnico.getId();
        return "Técnico ID: " + idTecnico
                + " (" + nombreTecnico + ")"
                + " | Carga actual: " + cargaActual
                + " | Especialidad coincide: " + (especialidadCoincide ? "Sí" : "No")
                + " | Criterio: " + criterioAplicado;
    }
}
