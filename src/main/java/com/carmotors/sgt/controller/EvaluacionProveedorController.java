package com.carmotors.sgt.controller;

import java.math.BigDecimal;
import java.util.List;

import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.RankingProveedor;
import com.carmotors.sgt.service.EvaluacionProveedorService;

public class EvaluacionProveedorController {

    private final EvaluacionProveedorService evaluacionProveedorService;

    public EvaluacionProveedorController() {
        this(new EvaluacionProveedorService());
    }

    public EvaluacionProveedorController(EvaluacionProveedorService evaluacionProveedorService) {
        if (evaluacionProveedorService == null) {
            throw new IllegalArgumentException("El servicio de evaluaciones es obligatorio.");
        }
        this.evaluacionProveedorService = evaluacionProveedorService;
    }

    public List<EvaluacionProveedor> listarTodas() {
        return evaluacionProveedorService.listarTodas();
    }

    public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
        return evaluacionProveedorService.listarPorProveedor(proveedorId);
    }

    public EvaluacionProveedor registrar(EvaluacionProveedor evaluacion) {
        return evaluacionProveedorService.registrarEvaluacion(evaluacion);
    }

    public BigDecimal obtenerPromedioProveedor(int proveedorId) {
        return evaluacionProveedorService.obtenerPromedioProveedor(proveedorId);
    }

    public List<RankingProveedor> generarRanking() {
        return evaluacionProveedorService.generarRanking();
    }
}
