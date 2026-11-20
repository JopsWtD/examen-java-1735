package com.carmotors.sgt.controller;

import java.util.List;

import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.service.ProveedorService;

public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController() {
        this(new ProveedorService());
    }

    public ProveedorController(ProveedorService proveedorService) {
        if (proveedorService == null) {
            throw new IllegalArgumentException("El servicio de proveedores es obligatorio.");
        }
        this.proveedorService = proveedorService;
    }

    public List<Proveedor> listarTodos() {
        return proveedorService.listarTodos();
    }

    public List<Proveedor> listarActivos() {
        return proveedorService.listarActivos();
    }

    public Proveedor buscarPorId(int id) {
        return proveedorService.buscarPorId(id);
    }

    public Proveedor guardar(Proveedor proveedor) {
        if (proveedor != null && proveedor.getId() > 0) {
            return proveedorService.actualizarProveedor(proveedor);
        }
        return proveedorService.crearProveedor(proveedor);
    }

    public Proveedor crear(Proveedor proveedor) {
        return proveedorService.crearProveedor(proveedor);
    }

    public Proveedor actualizar(Proveedor proveedor) {
        return proveedorService.actualizarProveedor(proveedor);
    }

    public Proveedor desactivar(int id) {
        return proveedorService.desactivarProveedor(id);
    }

    public Proveedor reactivar(int id) {
        return proveedorService.reactivarProveedor(id);
    }
}
