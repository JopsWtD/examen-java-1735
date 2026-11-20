package com.carmotors.sgt.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ResumenOrdenServicio {
    private final int id;
    private final int estadoId;
    private final String placa;
    private final LocalDateTime fechaRecepcion;
    private final Set<Integer> tiposServicio = new LinkedHashSet<>();
    private final Map<Integer, String> tecnicos = new LinkedHashMap<>();

    public ResumenOrdenServicio(int id, int estadoId, String placa, LocalDateTime fechaRecepcion) {
        this.id = id;
        this.estadoId = estadoId;
        this.placa = placa;
        this.fechaRecepcion = fechaRecepcion;
    }

    public int getId() { return id; }
    public int getEstadoId() { return estadoId; }
    public String getPlaca() { return placa; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    public Set<Integer> getTiposServicio() { return tiposServicio; }
    public Map<Integer, String> getTecnicos() { return tecnicos; }
}
