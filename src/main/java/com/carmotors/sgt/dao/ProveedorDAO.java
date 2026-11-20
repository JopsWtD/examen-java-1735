package com.carmotors.sgt.dao;

import java.util.List;

import com.carmotors.sgt.model.Proveedor;

public interface ProveedorDAO {

    List<Proveedor> listarTodos();

    List<Proveedor> listarActivos();

    Proveedor buscarPorId(int id);

    Proveedor buscarPorEmail(String email);

    int guardar(Proveedor proveedor);

    boolean actualizar(Proveedor proveedor);

    boolean desactivar(int id);

    boolean reactivar(int id);
}
