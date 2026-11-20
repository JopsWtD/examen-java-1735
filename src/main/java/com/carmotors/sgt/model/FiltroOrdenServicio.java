package com.carmotors.sgt.model;

import java.time.LocalDate;

public class FiltroOrdenServicio {
    private final Integer estadoId;
    private final Integer tecnicoId;
    private final String placa;
    private final Integer servicioTipoId;
    private final LocalDate desde;
    private final LocalDate hasta;

    public FiltroOrdenServicio(Integer estadoId, Integer tecnicoId, String placa, Integer servicioTipoId,
                              LocalDate desde, LocalDate hasta) {
        this.estadoId = estadoId;
        this.tecnicoId = tecnicoId;
        this.placa = placa;
        this.servicioTipoId = servicioTipoId;
        this.desde = desde;
        this.hasta = hasta;
    }

    public Integer getEstadoId() { return estadoId; }
    public Integer getTecnicoId() { return tecnicoId; }
    public String getPlaca() { return placa; }
    public Integer getServicioTipoId() { return servicioTipoId; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
}
