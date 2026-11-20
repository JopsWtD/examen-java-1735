package com.carmotors.sgt.controller;

import java.util.List;

import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.LotePorVencer;
import com.carmotors.sgt.service.LoteService;

public class LoteController {

    private final LoteService loteService;

    public LoteController() {
        this(new LoteService());
    }

    public LoteController(LoteService loteService) {
        if (loteService == null) {
            throw new IllegalArgumentException("El servicio de lotes es obligatorio.");
        }
        this.loteService = loteService;
    }

    public Lote registrar(Lote lote) {
        return loteService.registrarLote(lote);
    }

    public Lote buscarPorCodigo(String codigo) {
        return loteService.buscarPorCodigo(codigo);
    }

    public List<Lote> listarPorRepuesto(int repuestoId) {
        return loteService.listarPorRepuesto(repuestoId);
    }

    public List<LotePorVencer> generarReporteLotesPorVencer() {
        return loteService.generarReporteLotesPorVencer();
    }

    public List<LotePorVencer> generarReporteLotesPorVencer(int dias) {
        return loteService.generarReporteLotesPorVencer(dias);
    }
}
