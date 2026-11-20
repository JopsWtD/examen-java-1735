package com.carmotors.sgt.controller;

import java.math.BigDecimal;
import java.util.List;

import com.carmotors.sgt.model.ComparacionProveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.service.ProveedorRepuestoService;

public class ProveedorRepuestoController {

    private final ProveedorRepuestoService proveedorRepuestoService;

    public ProveedorRepuestoController() {
        this(new ProveedorRepuestoService());
    }

    public ProveedorRepuestoController(ProveedorRepuestoService proveedorRepuestoService) {
        if (proveedorRepuestoService == null) {
            throw new IllegalArgumentException("El servicio del catálogo proveedor-repuesto es obligatorio.");
        }
        this.proveedorRepuestoService = proveedorRepuestoService;
    }

    public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
        return proveedorRepuestoService.listarPorProveedor(proveedorId);
    }

    public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
        return proveedorRepuestoService.listarPorRepuesto(repuestoId);
    }

    public ProveedorRepuesto asociar(ProveedorRepuesto proveedorRepuesto) {
        return proveedorRepuestoService.asociar(proveedorRepuesto);
    }

    public ProveedorRepuesto actualizarCondiciones(int proveedorId, int repuestoId, BigDecimal costo,
                                                   int tiempoEntregaDias) {
        return proveedorRepuestoService.actualizarCondiciones(proveedorId, repuestoId, costo, tiempoEntregaDias);
    }

    public void quitarAsociacion(int proveedorId, int repuestoId) {
        proveedorRepuestoService.quitarAsociacion(proveedorId, repuestoId);
    }

    public ProveedorRepuesto marcarHabitual(int proveedorId, int repuestoId) {
        return proveedorRepuestoService.marcarHabitual(proveedorId, repuestoId);
    }

    public List<ComparacionProveedor> compararProveedoresPorRepuesto(int repuestoId) {
        return proveedorRepuestoService.compararProveedoresPorRepuesto(repuestoId);
    }
}
