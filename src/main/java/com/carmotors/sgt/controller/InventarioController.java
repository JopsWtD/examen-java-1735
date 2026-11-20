package com.carmotors.sgt.controller;

import java.util.List;

import com.carmotors.sgt.model.StockRepuesto;
import com.carmotors.sgt.service.InventarioService;

public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController() {
        this(new InventarioService());
    }

    public InventarioController(InventarioService inventarioService) {
        if (inventarioService == null) {
            throw new IllegalArgumentException("El servicio de inventario es obligatorio.");
        }
        this.inventarioService = inventarioService;
    }

    public StockRepuesto consultarStock(int repuestoId) {
        return inventarioService.calcularStock(repuestoId);
    }

    public List<StockRepuesto> consultarStockActivos() {
        return inventarioService.calcularStockActivos();
    }

    public List<StockRepuesto> generarReporteStockBajo() {
        return inventarioService.generarReporteStockBajo();
    }
}
