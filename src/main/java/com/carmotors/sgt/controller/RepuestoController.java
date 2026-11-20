package com.carmotors.sgt.controller;

import java.util.List;

import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.service.RepuestoService;

public class RepuestoController {

    private final RepuestoService repuestoService;

    public RepuestoController() {
        this(new RepuestoService());
    }

    public RepuestoController(RepuestoService repuestoService) {
        if (repuestoService == null) {
            throw new IllegalArgumentException("El servicio de repuestos es obligatorio.");
        }
        this.repuestoService = repuestoService;
    }

    public List<Repuesto> listarTodos() {
        return repuestoService.listarTodos();
    }

    public List<Repuesto> listarActivos() {
        return repuestoService.listarActivos();
    }

    public List<RepuestoTipo> listarTipos() {
        return repuestoService.listarTipos();
    }

    public String obtenerNombreTipo(int tipoId) {
        return repuestoService.obtenerNombreTipo(tipoId);
    }

    public Repuesto buscarPorId(int id) {
        return repuestoService.buscarPorId(id);
    }

    public List<Repuesto> buscarPorFiltros(String nombre, String tipo, String marca, String proveedor) {
        return repuestoService.buscarPorFiltros(nombre, tipo, marca, proveedor);
    }

    public Repuesto crear(Repuesto repuesto) {
        return repuestoService.crearRepuesto(repuesto);
    }

    public Repuesto actualizar(Repuesto repuesto) {
        return repuestoService.actualizarRepuesto(repuesto);
    }

    public Repuesto desactivar(int id) {
        return repuestoService.desactivarRepuesto(id);
    }

    public Repuesto reactivar(int id) {
        return repuestoService.reactivarRepuesto(id);
    }
}
